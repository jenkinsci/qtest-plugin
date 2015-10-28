qtest.init();
$j(document).ready(function () {
  setTimeout(function () {
    onLoadProject();
    onLoadProjectData();
    bindSelectizeChange();
  }, 1000)
});

function bindSelectizeChange() {
  qtest.bindSelectizeValue("input[name='config.projectName']", "input[name='config.projectId']", "id");
  qtest.bindSelectizeValue("input[name='config.releaseName']", "input[name='config.releaseId']", "id");
  qtest.bindSelectizeValue("input[name='config.environmentName']", "input[name='config.environmentId']", "value");
}

function onLoadProject() {
  $j("#fetchProject").on('click', function (e) {
    e.preventDefault();
    qtest.showLoading(this);
    loadProject();
  });
}

function loadProject() {
  var btn = $j("#fetchProject")[0];
  qtest.fetchProjects(function (data) {
    var projects = [];
    if (data.projects && data.projects != "") {
      projects = data.projects;
    }
    qtest.initSelectize("input[name='config.projectName']", 'selectizeProject', projects);

    //Saved configuration from qTest for this project of jenkins instance
    qtest.setting = {};
    if (data.setting && data.setting != "") {
      qtest.setting = data.setting;
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
}

function onLoadProjectData() {
  $j("#fetchProjectData").on('click', function (e) {
    e.preventDefault();
    qtest.showLoading(this);
    loadProjectData();
  });
}
function loadProjectData() {
  if (qtest.getProjectId() <= 0) {
    console.log("No project selected.")
    return;
  }
  var btn = $j("#fetchProjectData")[0];
  if (!qtest.setting) {
    loadProject();
  } else {
    qtest.fetchProjectData(function (data) {
      loadRelease(data);
      loadEnvironment(data);
      qtest.hideLoading(btn);
    }, function () {
      qtest.hideLoading(btn);
    })
  }
}

function loadRelease(data) {
  //load release
  var releases = [];
  if (data.releases && data.releases != "") {
    releases = data.releases;
  }
  qtest.initSelectize("input[name='config.releaseName']", 'selectizeRelease', releases);

  var selectedRelease = qtest.find(qtest.setting, "releaseId", qtest.setting.releaseId);
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

  var selectedEnvironment = qtest.find(qtest.setting, "environmentId", qtest.setting.environmentId);
  if (selectedEnvironment)
    qtest.selectizeEnvironment.setValue(selectedEnvironment.label);
}