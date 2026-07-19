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
    });
})();
