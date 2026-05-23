const searchInput = document.querySelector("[data-search]");

if (searchInput) {
    searchInput.addEventListener("input", () => {
        const searchText = searchInput.value.toLowerCase();
        document.querySelectorAll("[data-row]").forEach((row) => {
            row.hidden = !row.textContent.toLowerCase().includes(searchText);
        });
    });
}

document.querySelectorAll("[data-confirm-delete]").forEach((form) => {
    form.addEventListener("submit", (event) => {
        if (!confirm("Delete this record?")) {
            event.preventDefault();
        }
    });
});
