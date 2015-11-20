qtest.init();
$j(document).ready(function () {
  setTimeout(function () {
    disableTestBox(true);
    onLoadProject();
    bindSelectizeChange();
  }, 1000)
});

function bindSelectizeChange() {
  qtest.bindSelectizeValue("input[name='config.projectName1']", "input[name='config.projectId']",
    "input[name='config.projectName']", "id", "name", function (item) {
      loadProjectData();
    });
  qtest.bindSelectizeValue("input[name='config.releaseName1']", "input[name='config.releaseId']",
    "input[name='config.releaseName']", "id", "name");
  qtest.bindSelectizeValue("input[name='config.environmentName1']", "input[name='config.environmentId']",
    "input[name='config.environmentName']", "value", "label");
}

function disableTestBox(disable) {
  if (disable) {
    $j("input[name='config.projectName1']").attr('readonly', 'readonly');
    $j("input[name='config.releaseName1']").attr('readonly', 'readonly');
    $j("input[name='config.environmentName11']").attr('readonly', 'readonly');
  } else {
    $j("input[name='config.projectName1']").removeAttr('readonly');
    $j("input[name='config.releaseName1']").removeAttr('readonly');
    $j("input[name='config.environmentName1']").removeAttr('readonly');
  }
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
    if (projects.length <= 0) {
      //clear release & environment
      qtest.initSelectize("input[name='config.releaseName1']", 'selectizeRelease', []);
      qtest.initSelectize("input[name='config.environmentName1']", 'selectizeEnvironment', [],
        {
          create: true,
          valueField: 'value',
          labelField: 'label',
          searchField: 'label'
        });
    }

    qtest.initSelectize("input[name='config.projectName1']", 'selectizeProject', projects);

    //get current saved project:
    var configuredProjectId = $j("input[name='config.projectId']").val();
    var selectedProject = null;
    if (projects.length > 0) {
      selectedProject = configuredProjectId ? qtest.find(projects, 'id', configuredProjectId) : projects[0];
    }
    if (selectedProject)
      qtest.selectizeProject.setValue(selectedProject.id);
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
  $j.each(releases, function (index) {
    var item = releases[index];
    item.pidName = item.pid + " " + item.name;
  });
  qtest.initSelectize("input[name='config.releaseName1']", 'selectizeRelease', releases,
    {
      labelField: 'pidName',
      searchField: 'pidName'
    });

  var selectedRelease = qtest.find(releases, "id", qtest.setting.release_id);
  if (!selectedRelease) {
    selectedRelease = releases.length > 0 ? releases[0] : null;
  }
  if (selectedRelease)
    qtest.selectizeRelease.setValue(selectedRelease.id);
}

function loadEnvironment(data) {
  //load environment
  var environments = [];
  var fieldIsInActive = false;
  var hasInActiveValue = false;
  if (data.environments && data.environments != "") {
    fieldIsInActive = data.environments.is_active ? false : true;
    if (!fieldIsInActive) {
      //get allowed_values
      $j.each(data.environments.allowed_values, function (index) {
        var item = data.environments.allowed_values[index];
        if (item.is_active) {
          environments.push(item);
        } else {
          hasInActiveValue = true;
        }
      });
    }
    if (environments.length > 0)
      hasInActiveValue = false;
  }
  var show = fieldIsInActive || hasInActiveValue;
  $j("span[class='config.environmentName1']").attr('style', 'display:' + (show ? '' : 'none'));
  qtest.initSelectize("input[name='config.environmentName1']", 'selectizeEnvironment', environments,
    {
      create: true,
      valueField: 'value',
      labelField: 'label',
      searchField: 'label'
    });

  var selectedEnvironment = qtest.find(environments, "value", qtest.setting.environment_id);
  if (selectedEnvironment)
    qtest.selectizeEnvironment.setValue(selectedEnvironment.value);
}