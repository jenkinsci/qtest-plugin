var qtest = (function ($j) {
  var module = {};
  module.init = function () {
    console.log("qTest module created.");
  }
  var getUrl = function () {
    return $j("input[name='config.url']").val();
  };
  var getAppKey = function () {
    return $j("input[name='config.appSecretKey']").val();
  };
  module.getProjectId = function () {
    return $j("input[name='config.projectId']").val();
  };
  module.bindSelectizeValue = function (src, dest, field) {
    var srcNode = $j(src);
    srcNode.on('change', function () {
      var item = this.selectize.options[this.value];
      if (!item) return;
      console.log("selected value:", item);
      var destNode = $j(dest);
      destNode.val(destNode ? item[field] : null);
    });
  };
  module.initSelectize = function (inputName, selectizeId, data, options) {
    var selectizeNode = $j(inputName);
    var selectizeItem = qtest[selectizeId];
    if (selectizeItem) {
      selectizeItem.clear();
      selectizeItem.clearOptions();
      selectizeItem.addOption(data);
    } else {
      var opts = $j.extend({
        maxItems: 1,
        valueField: 'name',
        labelField: 'name',
        searchField: 'name',
        options: data,
        create: false
      }, options);
      var control = selectizeNode.selectize(opts);
      qtest[selectizeId] = control[0].selectize;
    }
    return qtest[selectizeId];
  };
  module.find = function (src, field, value) {
    var res = null;
    $j.each(src, function (index) {
      if (src[index][field] == value) {
        res = src[index];
        return res;
      }
    })
    return res;
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
    remoteAction.getProjects(getUrl(), getAppKey(), $j.proxy(function (t) {
      if (onSuccess)
        onSuccess(t.responseObject());
    }, this));
  };
  module.fetchProjectData = function (onSuccess, onError) {
    var jenkinsProjectName = $j("input[name='name']").val();
    remoteAction.getProjectData(getUrl(), getAppKey(), this.getProjectId(), jenkinsProjectName,
      $j.proxy(function (t) {
        if (onSuccess)
          onSuccess(t.responseObject());
      }, this));
  };
  return module;
}($j));

