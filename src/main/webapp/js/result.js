remoteAction.getTreeResult(10, $j.proxy(function (t) {
  var itemsResponse = t.responseObject();
  $j('#submittedResult').dataTable({
    data: itemsResponse.data,
    aoColumns: [
      {"mData": "buildNumber"},
      {"mData": "submitStatus"},
      {"mData": "testSuiteName"},
      {"mData": "numberTestResult"},
      {"mData": "numberTestLog"}
    ],
    order: [[0, "desc"]],
    aoColumnDefs: [{
      "aTargets": [0],
      "mData": "buildNumber",
      "mRender": function (data, type, full) {
        return '<a target="_blank" href="../' + data + '">' + data + '</a>';
      }
    }, {
      "aTargets": [2],
      "mData": "testSuiteName",
      "mRender": function (data, type, full) {
        return '<a target="_blank" href="' + full.testSuiteLink + '">' + data + '</a>';
      }
    }]
  });
}, this));