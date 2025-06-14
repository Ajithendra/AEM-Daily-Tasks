console.log("hi");
(function (document, $) {
    "use strict";

    // When dialog gets injected
    $(document).on("foundation-contentloaded", function (e) {
        // Handle initial values to show/hide target elements
        checkboxShowHideHandler($("cq-dialog-checkbox-showhide", e.target));
    });

    // Handle change event on the checkbox
    $(document).on("change", ".cq-dialog-checkbox-showhide", function (e) {
        checkboxShowHideHandler($(this));
    });

    /**
     * Handles showing/hiding elements based on the checkbox state.
     * @param {jQuery} el - Checkbox element(s)
     */
    function checkboxShowHideHandler(el) {
        el.each(function (i, element) {
            if ($(element).is("coral-checkbox")) {
                // Handle Coral3-based checkbox
                Coral.commons.ready(element, function (component) {
                    showHide(component, element);
                    component.on("change", function () {
                        showHide(component, element);
                    });
                });
            } else {
                // Handle Coral2-based checkbox
                var component = $(element).data("checkbox");
                if (component) {
                    showHide(component, element);
                }
            }
        });
    }

    /**
     * Show or hide target elements based on the checkbox state.
     * @param {Object} component - Checkbox component
     * @param {HTMLElement} element - The checkbox element
     */
    function showHide(component, element) {
        if (!element) return; // Guard clause to avoid null or undefined issues

        // Get the selector for target elements from the data attribute
        var target = $(element).data("cqDialogCheckboxShowhideTarget");
        if (!target) return;

        var $target = $(target);

        // Show or hide the target elements
        $target.addClass("hide");
        if (component.checked) {
            $target.removeClass("hide");
        }
    }
})(document, Granite.$);
