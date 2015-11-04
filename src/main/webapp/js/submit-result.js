remoteAction.getTreeResult(10, $j.proxy(function (t) {
  var itemsResponse = t.responseObject();
  $j('#submittedResult').dataTable({
    data: itemsResponse.data,
    aoColumns: [
      {"mData": "buildNumber"},
      {"mData": "submitStatus"},
      {"mData": "testSuiteName"},
      {"mData": "numberTestResult"},
      {"mData": "numberTestRun"}
    ],
    aoColumnDefs: [{
      "aTargets": [0],
      "mData": "buildNumber",
      "mRender": function (data, type, full) {
        return '<a target="_blank" href="../' + data + '">' + data + '</a>';
      }
    }]
  });
}, this));