function prefillCustomParameter(select) {
    const container = select.closest("[data-prefill-value]");
    if (!container) {
        return false;
    }

    const value = container.getAttribute("data-prefill-value");
    if (!value) {
        return false;
    }

    const option = Array.from(select.options).find((candidate) => candidate.value === value);
    if (!option) {
        return false;
    }

    select.value = value;
    return true;
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".build-parameter-custom select").forEach((select) => {
        if (prefillCustomParameter(select)) {
            return;
        }

        const observer = new MutationObserver(() => {
            if (prefillCustomParameter(select)) {
                observer.disconnect();
            }
        });
        observer.observe(select, { childList: true, subtree: true });
    });
});
