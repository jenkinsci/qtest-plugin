qtest.init();
$j(document).ready(function () {
  setTimeout(function () {
    disableTextBox(true);
    onLoadProject();
    bindSelectizeChange();
    hideNoHelp();
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
  qtest.bindSelectizeValue("input[name='config.containerTypeName1']", "input[name='config.containerTypeId']",
    "input[name='config.containerTypeName']", "id", "name");
}
/*Hide unexpected help icon for fields, cause jenkins auto make help url of radio block inherit by our publish action help url*/
function hideNoHelp() {
  var parent = $j("div[descriptorid='com.qasymphony.ci.plugin.action.PushingResultAction']");
  if (!parent || parent.length <= 0)
    return;
  var trNodes = parent.find("tr[class='radio-block-start '][hashelp='false'] > td[class='setting-help']");

  $j.each(trNodes, function (index) {
    var helpNode = trNodes[index];
    if (helpNode)
      helpNode.setAttribute('style', 'display:none');
  });
}

function disableTextBox(disable) {
  if (disable) {
    $j("input[name='config.projectName1']").attr('readonly', 'readonly');
    $j("input[name='config.releaseName1']").attr('readonly', 'readonly');
    $j("input[name='config.environmentName1']").attr('readonly', 'readonly');
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
    disableTextBox(false);
    loadProject();
  });
}
function clearProjectData() {
  //clear release & environment
  bindRelease([]);
  bindEnvironment([]);
}
function bindRelease(releases) {
  qtest.initSelectize("input[name='config.releaseName1']", 'selectizeRelease', releases,
    {
      labelField: 'name',
      searchField: ['pid', 'name'],
      render: {
        item: function (item, escape) {
          return '<div>' + escape(item.pid) + ' ' + escape(item.name) + '</div>';
        },
        option: function (item, escape) {
          return '<div>' + escape(item.pid) + ' ' + escape(item.name) + '</div>';
        }
      }
    });
}
function bindEnvironment(envs) {
  qtest.initSelectize("input[name='config.environmentName1']", 'selectizeEnvironment', envs,
    {
      create: true,
      valueField: 'value',
      labelField: 'label',
      searchField: 'label'
    });
}

function bindContainerType() {
  var containerTypes = [
    {
      id: 1,
      name: "Release"
    },
    {
      id: 2,
      name: "Test Cycle"
    },
    {
      id: 3,
      name: "Test Suite"
    }
  ];
  qtest.initSelectize("input[name='config.containerTypeName1']", 'selectizeContainerType', containerTypes);
  qtest.selectizeContainerType.setValue(containerTypes[0].id);
}

function loadProject() {
  clearProjectData();
  var btn = $j("#fetchProjectData")[0];
  qtest.fetchProjects(function (data) {
    var projects = [];
    if (data.projects && data.projects != "") {
      projects = data.projects;
    }

    qtest.initSelectize("input[name='config.projectName1']", 'selectizeProject', projects);

    //get current saved project:
    var configuredProjectId = $j("input[name='config.projectId']").val();
    var selectedProject = null;
    if (projects.length > 0) {
      selectedProject = configuredProjectId ? qtest.find(projects, 'id', configuredProjectId) : projects[0];
    }
    qtest.hideLoading(btn);
    if (selectedProject)
      qtest.selectizeProject.setValue(selectedProject.id);
  }, function () {
    qtest.hideLoading(btn);
  })
}

function loadProjectData() {
  clearProjectData();
  var btn = $j("#fetchProjectData")[0];
  if (qtest.getProjectId() <= 0) {
    qtest.hideLoading(btn);
    return;
  }
  qtest.showLoading(btn);
  qtest.fetchProjectData(function (data) {
    //Saved configuration from qTest for this project of jenkins instance
    qtest.setting = {};
    if (data.setting && data.setting != "") {
      qtest.setting = data.setting;
    }
    bindContainerType();
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
  bindRelease(releases);

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
  var show = fieldIsInActive || hasInActiveValue || environments.length <= 0;
  $j("span[class='config.environmentName1']").attr('style', 'display:' + (show ? '' : 'none'));
  bindEnvironment(environments);

  var selectedEnvironment = qtest.find(environments, "value", qtest.setting.environment_id);
  if (selectedEnvironment)
    qtest.selectizeEnvironment.setValue(selectedEnvironment.value);
}