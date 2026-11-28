const upcomingTab = document.getElementById("upcoming-tab");
const pastTab = document.getElementById("past-tab");

const upcomingPanel = document.getElementById("upcoming-panel");
const pastPanel = document.getElementById("past-panel");

//check that the tab elements exist before adding click events
if (upcomingTab && pastTab && upcomingPanel && pastPanel) {

    //Switch to the upcoming appointments tab
    upcomingTab.addEventListener("click", function () {

        upcomingPanel.hidden = false;
        pastPanel.hidden = true;

        upcomingTab.classList.remove("btn-outline-primary");
        upcomingTab.classList.add("btn-primary");

        pastTab.classList.remove("btn-primary");
        pastTab.classList.add("btn-outline-primary");

        upcomingTab.setAttribute("aria-selected", "true");
        pastTab.setAttribute("aria-selected", "false");
    });

    //Switch to the past appointment tab
    pastTab.addEventListener("click", function () {

        pastPanel.hidden = false;
        upcomingPanel.hidden = true;

        pastTab.classList.remove("btn-outline-primary");
        pastTab.classList.add("btn-primary");

        upcomingTab.classList.remove("btn-primary");
        upcomingTab.classList.add("btn-outline-primary");

        pastTab.setAttribute("aria-selected", "true");
        upcomingTab.setAttribute("aria-selected", "false");
    });
}