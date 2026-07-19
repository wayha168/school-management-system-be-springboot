(function () {
    document.addEventListener("DOMContentLoaded", function () {
        if (typeof Chart === "undefined" || !window.__dashboardCharts) return;
        var data = window.__dashboardCharts;

        var people = document.getElementById("peopleChart");
        if (people) {
            new Chart(people, {
                type: "bar",
                data: {
                    labels: data.labels || [],
                    datasets: [
                        {
                            label: "Count",
                            data: (data.values || []).map(Number),
                            backgroundColor: [
                                "#93c5fd",
                                "#6ee7b7",
                                "#fdba74",
                                "#c4b5fd",
                                "#86efac",
                                "#67e8f9"
                            ],
                            borderRadius: 8
                        }
                    ]
                },
                options: {
                    responsive: true,
                    plugins: { legend: { display: false } },
                    scales: {
                        y: { beginAtZero: true, ticks: { precision: 0 } }
                    }
                }
            });
        }

        var role = document.getElementById("roleChart");
        if (role) {
            new Chart(role, {
                type: "doughnut",
                data: {
                    labels: ["Teachers", "Students"],
                    datasets: [
                        {
                            data: [Number(data.teachers || 0), Number(data.students || 0)],
                            backgroundColor: ["#34d399", "#fb923c"],
                            borderWidth: 0
                        }
                    ]
                },
                options: {
                    responsive: true,
                    plugins: { legend: { position: "bottom" } }
                }
            });
        }

        var term = document.getElementById("termChart");
        if (term) {
            new Chart(term, {
                type: "bar",
                data: {
                    labels: data.termLabels || [],
                    datasets: [
                        {
                            label: "Avg %",
                            data: (data.termValues || []).map(Number),
                            backgroundColor: ["#60a5fa", "#f59e0b", "#34d399", "#a78bfa", "#f472b6"],
                            borderRadius: 8
                        }
                    ]
                },
                options: {
                    responsive: true,
                    plugins: { legend: { display: false } },
                    scales: {
                        y: { beginAtZero: true, max: 100 }
                    }
                }
            });
        }

        var attendance = document.getElementById("attendanceMonthChart");
        if (attendance) {
            new Chart(attendance, {
                type: "line",
                data: {
                    labels: data.attendanceLabels || [],
                    datasets: [
                        {
                            label: "Present (P)",
                            data: (data.attendancePresent || []).map(Number),
                            borderColor: "#34d399",
                            backgroundColor: "rgba(52, 211, 153, 0.15)",
                            tension: 0.3,
                            fill: true
                        },
                        {
                            label: "Absent (A)",
                            data: (data.attendanceAbsent || []).map(Number),
                            borderColor: "#f87171",
                            backgroundColor: "rgba(248, 113, 113, 0.12)",
                            tension: 0.3,
                            fill: true
                        },
                        {
                            label: "Late / Excused (L)",
                            data: (data.attendanceLate || []).map(Number),
                            borderColor: "#fbbf24",
                            backgroundColor: "rgba(251, 191, 36, 0.12)",
                            tension: 0.3,
                            fill: true
                        }
                    ]
                },
                options: {
                    responsive: true,
                    plugins: { legend: { position: "bottom" } },
                    scales: {
                        y: { beginAtZero: true, ticks: { precision: 0 } }
                    }
                }
            });
        }

        var classAttendance = document.getElementById("classAttendanceMonthChart");
        if (classAttendance && window.__classAttendanceCharts) {
            var c = window.__classAttendanceCharts;
            new Chart(classAttendance, {
                type: "line",
                data: {
                    labels: c.labels || [],
                    datasets: [
                        {
                            label: "Present (P)",
                            data: (c.present || []).map(Number),
                            borderColor: "#34d399",
                            tension: 0.3,
                            fill: false
                        },
                        {
                            label: "Absent (A)",
                            data: (c.absent || []).map(Number),
                            borderColor: "#f87171",
                            tension: 0.3,
                            fill: false
                        },
                        {
                            label: "Late / Excused (L)",
                            data: (c.late || []).map(Number),
                            borderColor: "#fbbf24",
                            tension: 0.3,
                            fill: false
                        }
                    ]
                },
                options: {
                    responsive: true,
                    plugins: { legend: { position: "bottom" } },
                    scales: {
                        y: { beginAtZero: true, ticks: { precision: 0 } }
                    }
                }
            });
        }
    });
})();
