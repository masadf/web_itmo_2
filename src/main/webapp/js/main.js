import {validate} from "./validation.js";
import {hideError, renderForm, renderTable, showError} from "./front-view.js";

$("#values-form").submit((e) => {
    e.preventDefault();
    if (!validate()) return;

    $.ajax({
        url: "http://localhost:8081/",
        method: "GET",
        data: `${$("#values-form").serialize()}&timezone=${new Date().getTimezoneOffset()}`,
        beforeSend: () => {
            setButtonDisabled(true);
        },
        success: (data) => {
            setButtonDisabled(false);
            renderTable(data);

            let graph = $(".area-wrapper");
            let xVal = $("input[name=xVal]:checked").val();
            let yVal = $("input[name=yVal]").val();
            let rVal = $("input[name=rVal]:checked").val();
            let k = graph.width() / 2;

            graph.append(`<div class="dot" style="top: ${k - yVal * k / (1.25 * rVal) - 4}px; left: ${k + xVal * k / (1.25 * rVal) - 4}px" />`);
        }
    });
})

function setButtonDisabled(isDisabled) {
    $("button[type=submit]").attr("disabled", isDisabled);
}

$("input[name=xval]").on("change", (e) => {
    let value = e.currentTarget.defaultValue;

    $("input[name=xval]").map((index, item) => {
        if (item.defaultValue !== value) {
            item.checked = false;
        }
    })
})

$(window).on("load", (e) => {
    renderForm();
});

$("#remove_button").on("click", (e) => {
    $.ajax({
        url: "php/cleaner.php",
        method: "GET",
        dataType: "json",
        success: () => {
            // todo
        }
    });
})

$(".area-wrapper").on("click", (e) => {
    hideError();
    let rInput = $("input[name=rVal]:checked");

    if (!rInput.length) {
        showError("Параметр R не задан!");
        return;
    }

    let k = $(".area-wrapper").width() / 2;
    let rVal = rInput.val();
    let xVal = rVal * (e.offsetX - k) * 1.25 / k;
    let yVal = rVal * (k - e.offsetY) * 1.25 / k;

    $.ajax({
        url: "http://localhost:8081/",
        method: "GET",
        data: `xVal=${xVal}&yVal=${yVal}&rVal=${rVal}&timezone=${new Date().getTimezoneOffset()}`,
        beforeSend: () => {
            setButtonDisabled(true);
        },
        success: (data) => {
            setButtonDisabled(false);
            renderTable(data);

            let graph = $(".area-wrapper");

            graph.append(`<div class="dot" style="top: ${e.offsetY - 4}px; left: ${e.offsetX - 4}px;" />`);
        },
    });
})