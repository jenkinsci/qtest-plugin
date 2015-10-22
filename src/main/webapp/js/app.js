qTestJenkin.init();
remoteAction.getProjects(qTestJenkin.getUrl(), qTestJenkin.getAppKey(), $j.proxy(function (t) {
  var itemsResponse = t.responseObject();
  console.log("data:", itemsResponse);
}, this));

$j("#fetchProject").click(function (e) {
  e.preventDefault();
  var btn = this;
  qTestJenkin.showLoading(this);
  qTestJenkin.fetchProjects(function (data) {
    //var projectList = $j('#projectList');
    var projectList = $j("input[name='config.projectName']");
    projectList.html('');

    projectList.selectize({
      maxItems: 1,
      valueField: 'name',
      labelField: 'name',
      searchField: 'name',
      options: data,
      create: false
    });
    //iterate over the data and append a select option
    //$j.each(data, function (key, val) {
    //  projectList.append('<option value="' + val.id + '">' + val.name + '</option>');
    //});
    //select first project
    if (data && data.length > 0) {
      qTestJenkin.projectId = data[0].id;
    }
    qTestJenkin.hideLoading(btn);
  }, function () {
    qTestJenkin.hideLoading(btn);
  })
});

$j("#fetchProjectData").click(function (e) {
  e.preventDefault();
  qTestJenkin.showLoading(this);
  if (qTestJenkin.projectId <= 0) {
    console.log("No project selected.")
    return;
  }
  qTestJenkin.fetchModules(function (data) {
    var projectModuleList = $j("input[name='config.releaseName']");
    projectModuleList.html('');
    //iterate over the data and append a select option
    //$j.each(data, function (key, val) {
    //  projectModuleList.append('<option value="' + val.id + '">' + val.pid + ' ' + val.name + '</option>');
    //})

    projectModuleList.selectize({
      maxItems: 1,
      valueField: 'name',
      labelField: 'name',
      searchField: 'name',
      options: data,
      create: false
    });
  }, function () {
  })
  qTestJenkin.hideLoading(this);
});
var projectId = $j("input[name='config.projectName']");
projectId.on('change', function () {
  var item = this.selectize.options[this.value];
  if (!item) return;
  qTestJenkin.projectId = item.id;
  console.log("selected:", item.name);
  var projectName = $j("input[name='config.projectId']");
  projectName.val(item.id);
});
//var projectName = $j("input[name='projectName']");
//if (projectName && projectName.val()) {
//  projectId.val(projectName.val());
//}

var release = $j("input[name='config.releaseName']");
release.on('change', function () {
  var item = this.selectize.options[this.value];
  if (!item) return;
  var releaseName = $j("input[name='config.releaseId']");
  console.log("selected release:", item.name);
  releaseName.val(item.id);
});
//var releaseName = $j("input[name='releaseName']");
//if (releaseName && releaseName.val()) {
//  release.val(releaseName.val());
//}

