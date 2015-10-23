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
    ]
  });
}, this));