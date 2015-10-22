var qTestJenkin = (function ($j) {
  var module = {};
  module.projectId = 0;
  module.basedUrl = "https://qtest-dev.qtestnet.com";
  var accessToken = "bmVwaGVsZXxuZXBoZWxlbG9jYWxAZ21haWwuY29tOjE0NzY1MTM2NzU0ODM6MjdiNjRiMzUwMGEzNDAyYzY5Y2RiZTQ0M2QyZjQwY2U";
  module.init = function () {
    console.log("qTest module created.");
  }
  module.getUrl = function () {
    return module.basedUrl;
  }
  module.getAppKey = function () {
    return accessToken;
  }
  module.get = function (url, onSuccess, onError) {
    $j.ajax({
      url: url,
      dataType: 'JSON',
      headers: {
        'Authorization': accessToken,
        'Content-Type': 'application/json'
      },
      method: 'GET',
      success: function (data) {
        if (onSuccess)
          onSuccess(data);
      },
      error: function () {
        if (onError)
          onError();
      }
    });
  };
  module.showLoading = function (node) {
    if (!node) return;
    node.parentElement.next().style.display = '';
  };
  module.hideLoading = function (node) {
    if (!node) return;
    node.parentElement.next().style.display = 'none';
  };
  module.fetchProjects = function (onSuccess, onError) {
    return this.get(this.basedUrl + '/api/v3/projects', onSuccess, onError);
  };
  module.fetchModules = function (onSuccess, onError) {
    var url = this.basedUrl + '/api/v3/projects/' + this.projectId + "/releases";
    return this.get(url, onSuccess, onError);
  };
  return module;
}($j));

