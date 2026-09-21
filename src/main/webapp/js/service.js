var $j = jQuery.noConflict();
var qtest = (function ($j) {
  var module = {};
  module.init = function () {
  };
  var getUrl = function () {
    return $j("input[name='config.url']").val();
  };
  var getAppKey = function () {
    return $j("input[name='config.appSecretKey']").val();
  };
  var getSecretKey = function () {
    return $j("input[name='config.secretKey']").val();
  };
  module.getProjectId = function () {
    return $j("input[name='config.projectId']").val();
  };
  // Selectize's own "change" event (fired via instance.on('change', ...)) is what actually reflects
  // a selection, whether made by a real click or setValue(); the bundled selectize.min.js does not also
  // dispatch a native DOM "change" event on the underlying <input>, so binding via jQuery on the raw
  // node (as done previously) never fires. Since the selectize instance doesn't exist yet when this is
  // called (document-ready, before Retrieve Data has run), the sync spec is registered here and wired
  // up by initSelectize once the instance is actually created.
  var selectizeChangeBindings = {};
  module.bindSelectizeValue = function (src, dest, dest2, field, field2, onChange) {
    selectizeChangeBindings[src] = {dest: dest, dest2: dest2, field: field, field2: field2, onChange: onChange};
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
        valueField: 'id',
        labelField: 'name',
        searchField: 'name',
        options: data,
        create: false,
        enableCreateDuplicate: true
      }, options);
      var control = selectizeNode.selectize(opts);
      qtest[selectizeId] = control[0].selectize;
      if (!data || data.length <= 0) {
        qtest[selectizeId].clear();
        qtest[selectizeId].clearOptions();
      }
      var binding = selectizeChangeBindings[inputName];
      if (binding) {
        qtest[selectizeId].on('change', function (value) {
          var item = this.options[value];
          if (!item) return;
          var destNode = $j(binding.dest);
          destNode.val(destNode ? item[binding.field] : null);
          var destNode2 = $j(binding.dest2);
          destNode2.val(destNode2 ? item[binding.field2] : null);
          if (binding.onChange)
            binding.onChange(item);
        });
      }
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
    });
    return res;
  };
  module.showLoading = function (node) {
    if (!node) return;
    node.parentElement.nextElementSibling.style.display = '';
  };

  module.hideLoading = function (node) {
    if (!node) return;
    node.parentElement.nextElementSibling.style.display = 'none';
  };

  module.fetchProjects = function (onSuccess, onError) {
    console.log("fetchProjects");
    remoteAction.getProjects(getUrl(), getAppKey(), getSecretKey(), $j.proxy(function (t) {
      if (onSuccess)
        onSuccess(t.responseObject());
    }, this));
  };
  module.fetchProjectData = function (onSuccess, onError) {
    var jenkinsProjectName = $j("input[name='name']").val() || "";
    remoteAction.getProjectData(getUrl(), getAppKey(), getSecretKey(), this.getProjectId(), jenkinsProjectName,
      $j.proxy(function (t) {
        if (onSuccess)
          onSuccess(t.responseObject());
      }, this));
  };

  module.getContainerChildren = function (parentId, parentType, onSuccess, onError) {
    remoteAction.getContainerChildren(getUrl(), getAppKey(), getSecretKey(), this.getProjectId(), parentId, parentType,
        $j.proxy(function(t) {
            if (onSuccess){
                onSuccess(t.responseObject());
            }
        }, this));
    };
  module.getQtestInfo = function(url, onSuccess) {
    remoteAction.getQtestInfo(url, $j.proxy(function(t) {
         if (onSuccess){
             onSuccess(t.responseObject());
         }
    }, this));
  };

  return module;
}($j));

