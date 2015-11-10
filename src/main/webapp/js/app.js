qtest.init();
$j(document).ready(function () {
  setTimeout(function () {
    disableTestBox(true);
    onLoadProject();
    bindSelectizeChange();
  }, 1000)
});

function bindSelectizeChange() {
  qtest.bindSelectizeValue("input[name='config.projectName']", "input[name='config.projectId']", "id", function (item) {
    loadProjectData();
  });
  qtest.bindSelectizeValue("input[name='config.releaseName']", "input[name='config.releaseId']", "id");
  qtest.bindSelectizeValue("input[name='config.environmentName']", "input[name='config.environmentId']", "value");
}

function disableTestBox(disable){
  $j("input[name='config.projectName']").attr('disabled', disable);
  $j("input[name='config.releaseName']").attr('disabled', disable);
  $j("input[name='config.environmentName']").attr('disabled', disable);
}

function onLoadProject() {
  $j("#fetchProjectData").on('click', function (e) {
    e.preventDefault();
    qtest.showLoading(this);
    disableTestBox(false);
    loadProject();
  });
}

function loadProject() {
  var btn = $j("#fetchProjectData")[0];
  qtest.fetchProjects(function (data) {
    var projects = [];
    if (data.projects && data.projects != "") {
      projects = data.projects;
    }
    qtest.initSelectize("input[name='config.projectName']", 'selectizeProject', projects);

    var selectedProject = projects.length > 0 ? projects[0] : null;
    if (selectedProject)
      qtest.selectizeProject.setValue(selectedProject.name);
    //loadProjectData();
    qtest.hideLoading(btn);
  }, function () {
    qtest.hideLoading(btn);
  })
}

function loadProjectData() {
  if (qtest.getProjectId() <= 0) {
    console.log("No project selected.")
    return;
  }
  var btn = $j("#fetchProjectData")[0];
  qtest.fetchProjectData(function (data) {
    //Saved configuration from qTest for this project of jenkins instance
    qtest.setting = {};
    if (data.setting && data.setting != "") {
      qtest.setting = data.setting;
    }
    loadRelease(data);
    loadEnvironment(data);
    qtest.hideLoading(btn);
  }, function () {
    qtest.hideLoading(btn);
  })
}

function loadRelease(data) {
  //load release
  var releases = [];
  if (data.releases && data.releases != "") {
    releases = data.releases;
  }
  qtest.initSelectize("input[name='config.releaseName']", 'selectizeRelease', releases);

  var selectedRelease = qtest.find(releases, "id", qtest.setting.release_id);
  if (!selectedRelease) {
    selectedRelease = releases.length > 0 ? releases[0] : null;
  }
  if (selectedRelease)
    qtest.selectizeRelease.setValue(selectedRelease.name);
}

function loadEnvironment(data) {
//load environment
  var environments = [];
  if (data.environments && data.environments != "") {
    environments = data.environments;
  }
  qtest.initSelectize("input[name='config.environmentName']", 'selectizeEnvironment', environments,
    {
      create: true,
      valueField: 'label',
      labelField: 'label',
      searchField: 'label'
    });

  var selectedEnvironment = qtest.find(environments, "value", qtest.setting.environment_id);
  if (selectedEnvironment)
    qtest.selectizeEnvironment.setValue(selectedEnvironment.label);
}