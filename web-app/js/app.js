$('#dateOfApplication').datepicker({
    dateFormat: 'yy-mm-dd',
    minDate: ($('#type').val() == 'common') ? +3 : 0
});

$('#dateSelected, #date, #requestFromDate, #requestToDate').datepicker({
    dateFormat: 'yy-mm-dd'
});

$('#fromDate').datepicker({
    dateFormat: 'yy-mm-dd',
    minDate:0
});

$('#toDate').datepicker({
    dateFormat: 'yy-mm-dd',
    minDate:0
});
