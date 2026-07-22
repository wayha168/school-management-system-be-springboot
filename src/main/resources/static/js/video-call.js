(function () {
    const panel = document.querySelector(".video-call-panel");
    if (!panel) return;

    const roomCode = (panel.dataset.roomCode || "").toUpperCase();
    const peerId = panel.dataset.peerId;
    const displayName = panel.dataset.displayName || "User";
    const meetingId = panel.dataset.meetingId;
    const meetingActive = panel.dataset.meetingActive === "true";
    const canRecord = panel.dataset.canRecord === "true";
    const joinPath = panel.dataset.joinPath || ("/admin/classroom/call/" + roomCode);

    const localVideo = document.getElementById("local-video");
    const videoGrid = document.getElementById("video-grid");
    const statusEl = document.getElementById("call-status");
    const btnJoin = document.getElementById("btn-join-call");
    const btnMute = document.getElementById("btn-mute");
    const btnCam = document.getElementById("btn-cam");
    const btnLeave = document.getElementById("btn-leave");
    const btnRecordStart = document.getElementById("btn-record-start");
    const btnRecordStop = document.getElementById("btn-record-stop");
    const btnCopy = document.getElementById("copy-join-link");

    const peers = new Map(); // peerId -> { pc, video, tile }
    let localStream = null;
    let stompClient = null;
    let joined = false;
    let muted = false;
    let camOff = false;
    let mediaRecorder = null;
    let recordedChunks = [];

    function setStatus(text) {
        if (statusEl) statusEl.textContent = text;
    }

    function iceServers() {
        return [{ urls: "stun:stun.l.google.com:19302" }];
    }

    function sendSignal(payload) {
        if (!stompClient || !stompClient.connected) return;
        payload.roomCode = roomCode;
        payload.fromPeerId = peerId;
        payload.displayName = displayName;
        stompClient.send("/app/call/signal", {}, JSON.stringify(payload));
    }

    function ensureTile(remotePeerId, name) {
        let entry = peers.get(remotePeerId);
        if (entry && entry.tile) return entry;
        const tile = document.createElement("div");
        tile.className = "video-tile";
        tile.dataset.peerId = remotePeerId;
        const video = document.createElement("video");
        video.autoplay = true;
        video.playsInline = true;
        const label = document.createElement("span");
        label.className = "video-label";
        label.textContent = name || remotePeerId.slice(0, 8);
        tile.appendChild(video);
        tile.appendChild(label);
        videoGrid.appendChild(tile);
        entry = entry || {};
        entry.tile = tile;
        entry.video = video;
        entry.displayName = name;
        peers.set(remotePeerId, entry);
        return entry;
    }

    function removePeer(remotePeerId) {
        const entry = peers.get(remotePeerId);
        if (!entry) return;
        if (entry.pc) {
            try { entry.pc.close(); } catch (e) { /* ignore */ }
        }
        if (entry.tile && entry.tile.parentNode) {
            entry.tile.parentNode.removeChild(entry.tile);
        }
        peers.delete(remotePeerId);
    }

    async function createPeerConnection(remotePeerId, remoteName, isInitiator) {
        let entry = ensureTile(remotePeerId, remoteName);
        if (entry.pc) {
            try { entry.pc.close(); } catch (e) { /* ignore */ }
        }
        const pc = new RTCPeerConnection({ iceServers: iceServers() });
        entry.pc = pc;
        peers.set(remotePeerId, entry);

        if (localStream) {
            localStream.getTracks().forEach((track) => pc.addTrack(track, localStream));
        }

        pc.onicecandidate = (event) => {
            if (!event.candidate) return;
            sendSignal({
                type: "ice",
                toPeerId: remotePeerId,
                candidate: event.candidate.candidate,
                sdpMid: event.candidate.sdpMid,
                sdpMLineIndex: event.candidate.sdpMLineIndex
            });
        };

        pc.ontrack = (event) => {
            const stream = event.streams[0] || new MediaStream([event.track]);
            entry.video.srcObject = stream;
        };

        pc.onconnectionstatechange = () => {
            if (pc.connectionState === "failed" || pc.connectionState === "closed" || pc.connectionState === "disconnected") {
                // keep tile; reconnect may happen via re-join
            }
        };

        if (isInitiator) {
            const offer = await pc.createOffer();
            await pc.setLocalDescription(offer);
            sendSignal({ type: "offer", toPeerId: remotePeerId, sdp: offer.sdp });
        }
        return pc;
    }

    async function handleSignal(msg) {
        if (!msg || !msg.fromPeerId || msg.fromPeerId === peerId) return;
        if (msg.toPeerId && msg.toPeerId !== peerId) return;

        const from = msg.fromPeerId;
        const name = msg.displayName || from.slice(0, 8);

        if (msg.type === "join") {
            await createPeerConnection(from, name, true);
            return;
        }
        if (msg.type === "leave") {
            removePeer(from);
            return;
        }
        if (msg.type === "offer") {
            const pc = await createPeerConnection(from, name, false);
            await pc.setRemoteDescription({ type: "offer", sdp: msg.sdp });
            const answer = await pc.createAnswer();
            await pc.setLocalDescription(answer);
            sendSignal({ type: "answer", toPeerId: from, sdp: answer.sdp });
            return;
        }
        if (msg.type === "answer") {
            const entry = peers.get(from);
            if (!entry || !entry.pc) return;
            await entry.pc.setRemoteDescription({ type: "answer", sdp: msg.sdp });
            return;
        }
        if (msg.type === "ice") {
            const entry = peers.get(from) || await createPeerConnection(from, name, false);
            if (!entry.pc || !msg.candidate) return;
            try {
                await entry.pc.addIceCandidate({
                    candidate: msg.candidate,
                    sdpMid: msg.sdpMid,
                    sdpMLineIndex: msg.sdpMLineIndex
                });
            } catch (e) {
                console.warn("ICE add failed", e);
            }
        }
    }

    function connectSignaling() {
        return new Promise((resolve, reject) => {
            if (typeof SockJS === "undefined" || typeof Stomp === "undefined") {
                reject(new Error("WebSocket libraries missing"));
                return;
            }
            const socket = new SockJS("/ws");
            const client = Stomp.over(socket);
            client.debug = null;
            client.connect({}, function () {
                stompClient = client;
                client.subscribe("/topic/call/" + roomCode, function (frame) {
                    try {
                        handleSignal(JSON.parse(frame.body));
                    } catch (e) {
                        console.warn(e);
                    }
                });
                resolve();
            }, function (err) {
                reject(err);
            });
        });
    }

    async function joinCall() {
        if (!meetingActive) {
            setStatus("Meeting has ended");
            return;
        }
        if (joined) return;
        setStatus("Starting camera…");
        try {
            localStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
            localVideo.srcObject = localStream;
            await connectSignaling();
            sendSignal({ type: "join" });
            joined = true;
            btnJoin.disabled = true;
            btnMute.disabled = false;
            btnCam.disabled = false;
            btnLeave.disabled = false;
            if (btnRecordStart && canRecord) btnRecordStart.disabled = false;
            setStatus("In call");
        } catch (e) {
            console.error(e);
            setStatus("Could not access camera/mic: " + (e.message || e));
        }
    }

    function leaveCall() {
        if (mediaRecorder && mediaRecorder.state !== "inactive") {
            mediaRecorder.stop();
        }
        sendSignal({ type: "leave" });
        peers.forEach((_, id) => removePeer(id));
        if (localStream) {
            localStream.getTracks().forEach((t) => t.stop());
            localStream = null;
        }
        localVideo.srcObject = null;
        if (stompClient) {
            try { stompClient.disconnect(() => {}); } catch (e) { /* ignore */ }
            stompClient = null;
        }
        joined = false;
        btnJoin.disabled = !meetingActive;
        btnMute.disabled = true;
        btnCam.disabled = true;
        btnLeave.disabled = true;
        if (btnRecordStart) btnRecordStart.disabled = true;
        if (btnRecordStop) {
            btnRecordStop.disabled = true;
            btnRecordStop.hidden = true;
        }
        if (btnRecordStart) btnRecordStart.hidden = false;
        setStatus("Left call");
    }

    function toggleMute() {
        if (!localStream) return;
        muted = !muted;
        localStream.getAudioTracks().forEach((t) => { t.enabled = !muted; });
        btnMute.textContent = muted ? "Unmute" : "Mute";
    }

    function toggleCam() {
        if (!localStream) return;
        camOff = !camOff;
        localStream.getVideoTracks().forEach((t) => { t.enabled = !camOff; });
        btnCam.textContent = camOff ? "Camera on" : "Camera off";
    }

    function startRecording() {
        if (!localStream || !canRecord) return;
        recordedChunks = [];
        const mime = MediaRecorder.isTypeSupported("video/webm;codecs=vp9,opus")
            ? "video/webm;codecs=vp9,opus"
            : "video/webm";
        try {
            mediaRecorder = new MediaRecorder(localStream, { mimeType: mime });
        } catch (e) {
            mediaRecorder = new MediaRecorder(localStream);
        }
        mediaRecorder.ondataavailable = (ev) => {
            if (ev.data && ev.data.size > 0) recordedChunks.push(ev.data);
        };
        mediaRecorder.onstop = () => uploadRecording();
        mediaRecorder.start(1000);
        if (btnRecordStart) btnRecordStart.hidden = true;
        if (btnRecordStop) {
            btnRecordStop.hidden = false;
            btnRecordStop.disabled = false;
        }
        setStatus("Recording…");
    }

    function stopRecording() {
        if (!mediaRecorder || mediaRecorder.state === "inactive") return;
        setStatus("Saving recording…");
        mediaRecorder.stop();
        if (btnRecordStop) btnRecordStop.disabled = true;
    }

    async function uploadRecording() {
        const blob = new Blob(recordedChunks, { type: "video/webm" });
        recordedChunks = [];
        if (blob.size === 0) {
            setStatus("Empty recording");
            resetRecordButtons();
            return;
        }
        const form = new FormData();
        form.append("file", blob, "meeting-" + roomCode + ".webm");
        try {
            const res = await fetch("/admin/classroom/meetings/" + meetingId + "/recording", {
                method: "POST",
                body: form,
                credentials: "same-origin"
            });
            if (!res.ok) {
                const text = await res.text();
                throw new Error(text || ("HTTP " + res.status));
            }
            setStatus("Recording saved");
            let watch = document.getElementById("btn-watch-recording");
            if (!watch) {
                watch = document.createElement("a");
                watch.id = "btn-watch-recording";
                watch.className = "btn btn-ghost";
                watch.target = "_blank";
                watch.rel = "noopener";
                watch.textContent = "Watch recording";
                const toolbar = document.querySelector(".video-call-toolbar");
                if (toolbar) toolbar.insertBefore(watch, statusEl);
            }
            watch.href = "/admin/classroom/meetings/" + meetingId + "/recording";
        } catch (e) {
            console.error(e);
            setStatus("Failed to save recording: " + (e.message || e));
        }
        resetRecordButtons();
    }

    function resetRecordButtons() {
        if (btnRecordStart) {
            btnRecordStart.hidden = false;
            btnRecordStart.disabled = !joined;
        }
        if (btnRecordStop) {
            btnRecordStop.hidden = true;
            btnRecordStop.disabled = true;
        }
    }

    if (btnJoin) btnJoin.addEventListener("click", joinCall);
    if (btnLeave) btnLeave.addEventListener("click", leaveCall);
    if (btnMute) btnMute.addEventListener("click", toggleMute);
    if (btnCam) btnCam.addEventListener("click", toggleCam);
    if (btnRecordStart) btnRecordStart.addEventListener("click", startRecording);
    if (btnRecordStop) btnRecordStop.addEventListener("click", stopRecording);
    if (btnCopy) {
        btnCopy.addEventListener("click", async () => {
            const url = window.location.origin + joinPath;
            try {
                await navigator.clipboard.writeText(url);
                setStatus("Join link copied");
            } catch (e) {
                prompt("Copy join link:", url);
            }
        });
    }

    window.addEventListener("beforeunload", () => {
        if (joined) sendSignal({ type: "leave" });
    });
})();
