(function (document, $) {
    "use strict";

    $(document).on("foundation-contentloaded", function () {
        // Function to handle show/hide logic for checkboxes
        function showHide(component, element) {
            console.log('showing');
            // get the selector to find the target elements. it's stored as a data- attribute
            var target = $(element).data("cqDialogShowhideTarget");
            var $target = $(target);

            if (target) {
                if (component.checked) {
                    $target.find('input, select, textarea, button').prop('disabled', false);
                } else {
                    $target.find('input, select, textarea, button').prop('disabled', true);
                    if ($target.find('input, select, textarea, button').prop('type') !== 'checkbox') {
                        $target.find('input, select, textarea, button').val(' '); // Set value to empty space if checkbox is unchecked
                    }
                }
            }
        }

        // Function to handle checkbox show/hide events
        function checkboxShowHideHandler(el) {
            el.each(function (i, element) {
                if ($(element).is("coral-checkbox")) {
                    // handle Coral3 base drop-down
                    Coral.commons.ready(element, function (component) {
                        showHide(component, element);
                        component.on("change", function () {
                            showHide(component, element);
                            var currentpagePath = window.location.pathname;
                            var fieldValue = ''; // you should define fieldValue variable
                            var apiUrl = "/bin/deleteCssClass?" + "path=" + currentpagePath;

                            // Check if fieldValue is empty or null
                            if (fieldValue == "" || fieldValue == null) {
                                $.ajax({
                                    url: apiUrl,
                                    type: 'GET',
                                    data: {
                                        currentpagePath: currentpagePath
                                    },
                                    success: function (data) {
                                        console.log('Success:', data);
                                    },
                                    error: function (xhr, status, error) {
                                        console.error('Error:', error);
                                    }
                                });
                            }
                        });
                    });
                } else {
                    // handle Coral2 based drop-down
                    var component = $(element).data("checkbox");
                    if (component) {
                        showHide(component, element);
                    }
                }
            });
        }

        // Event listener for when dialog gets injected
        $(document).on("foundation-contentloaded", function (e) {
            // if there is already an initial value make sure the according target element becomes visible
            checkboxShowHideHandler($(".cq-dialog-showhide", e.target));
        });

        // Event listener for checkbox change events
        $(document).on("change", ".cq-dialog-showhide", function (e) {
            checkboxShowHideHandler($(this));
        });
    });

})(document, Granite.$);
