qtest.init();
$j(document).ready(function () {
  setTimeout(function () {
    onLoadProject();
    onLoadProjectData();
    bindSelectizeChange();
  }, 1000)
});

function onLoadProject() {
  $j("#fetchProject").on('click', function (e) {
    e.preventDefault();
    var btn = this;
    qtest.showLoading(btn);
    qtest.fetchProjects(function (data) {
      var projects = [];
      if (data.projects && data.projects != "") {
        projects = JSON.parse(data.projects);
      }
      qtest.initSelectize("input[name='config.projectName']", 'selectizeProject', projects);

      //Saved configuration from qTest for this project of jenkins instance
      qtest.setting = {};
      if (data.setting && data.setting != "") {
        qtest.setting = JSON.parse(data.setting);
      }

      //TODO: query configured project.
      var selectedProject = qtest.find(qtest.setting, "projectId", qtest.setting.projectId);
      if (!selectedProject) {
        //select first project
        selectedProject = projects.length > 0 ? projects[0] : null;
      }
      if (selectedProject)
        qtest.selectizeProject.setValue(selectedProject.name);

      loadProjectData();

      qtest.hideLoading(btn);
    }, function () {
      qtest.hideLoading(btn);
    })
  });
}

function onLoadProjectData() {
  $j("#fetchProjectData").on('click', function (e) {
    e.preventDefault();
    var btn = this;
    qtest.showLoading(btn);
    loadProjectData(btn);
  });
}
function loadProjectData(btn) {
  if (qtest.getProjectId() <= 0) {
    console.log("No project selected.")
    return;
  }
  qtest.fetchProjectData(function (data) {
    //load release
    var releases = [];
    if (data.releases && data.releases != "") {
      releases = JSON.parse(data.releases);
    }
    qtest.initSelectize("input[name='config.releaseName']", 'selectizeRelease', releases);

    var selectedRelease = qtest.find(qtest.setting, "releaseId", qtest.setting.releaseId);
    if (!selectedRelease) {
      selectedRelease = releases.length > 0 ? releases[0] : null;
    }
    if (selectedRelease)
      qtest.selectizeRelease.setValue(selectedRelease.name);

    //load environment
    var environments = [];
    if (data.environments && data.environments != "") {
      environments = JSON.parse(data.environments);
    }
    qtest.initSelectize("input[name='config.environment']", 'selectizeEnvironment', environments, true);

    var selectedEnvironment = qtest.find(qtest.setting, "environmentId", qtest.setting.environmentId);
    if (!selectedEnvironment) {
      selectedEnvironment = environments.length > 0 ? environments[0] : null;
    }
    if (selectedEnvironment)
      qtest.selectizeEnvironment.setValue(selectedEnvironment.name);

    qtest.hideLoading(btn);
  }, function () {
    qtest.hideLoading(btn);
  })
}

function bindSelectizeChange() {
  qtest.bindSelectizeValue("input[name='config.projectName']", "input[name='config.projectId']", "id");
  qtest.bindSelectizeValue("input[name='config.releaseName']", "input[name='config.releaseId']", "id");
}