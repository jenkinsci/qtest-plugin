qtest.init();
$j(document).ready(function () {
  setTimeout(function () {
    disableTextBox(true);
    onLoadProject();
    bindSelectizeChange();
    hideNoHelp();
  }, 1000);
  $j(document).on("click", ".collapse-indicator, .expand-indicator", function(event) {
    //console.log(event);
    var toggleSubItem = (jIndicatorItem, jSubContent) => {
        jSubContent.slideToggle(500, function() {
            var className =  jSubContent.is(":visible") ?  "expand-indicator": "collapse-indicator";
            changeIndicator(jIndicatorItem, className);
        });
    }
    try {

        if (event.currentTarget) {
            if (event.currentTarget.hasAttribute("requested")){
                toggleSubItem($j(event.currentTarget), $j(event.currentTarget.parentElement.next()));
            } else {
                changeIndicator($j(event.currentTarget), "loading-indicator");
                var contentItem = event.currentTarget.parentElement.querySelector("div[class='content']");
                var parentId = contentItem.getAttribute("qtest.id");
                var parentType = contentItem.getAttribute("qtest.type");
                qtest.getContainerChildren(parentId, parentType, function(data) {
                    loadContainers($j(event.currentTarget.parentElement.next()), data);
                    if (!data || 0 === (data.testSuites.length + data.testCycles.length)) {
                        changeIndicator($j(event.currentTarget), "empty-indicator");
                        return;
                    }
                    toggleSubItem($j(event.currentTarget), $j(event.currentTarget.parentElement.next()));
                });
                event.currentTarget.setAttribute("requested", "true");
            }
        }


    } catch (ex) {
        console.error(ex);
    }

  });
});

function changeIndicator(jNode, className) {
    jNode.removeClass();
    jNode.addClass(className);
}
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
  $j('#containerTree').empty();
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
    loadRelease(data);
    loadEnvironment(data);
    loadContainers($j('#containerTree'), data);
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

function buildTree(jItem, data) {
    if (data && data.length > 0) {
        var ul = $j( "<ul></ul>" );
        data.forEach(function(element) {
            var li = $j( "<li></li>" );
            var divItemContainer = $j("<div class='item-container'></div>");
            var divMainItem = $j("<div class='main-item'></div>");
            var divSubItems = $j("<div class='sub-item' style='display: none;'></div>");
            divItemContainer.append(divMainItem);
            divItemContainer.append(divSubItems);

            if (element.type !== 'test-suite') {
                divMainItem.append("<span class='collapse-indicator' style='align-self: center;'></span>");
            } else {
                divMainItem.append("<span class='empty-indicator'></span>");
            }
            var divLink = $j("<a target='_blank'></a>")
            divLink.attr("href", element.web_url)
            divMainItem.append(divLink);
            var divContent = $j("<div class='content'></div>");
            divContent.text(element.name);
            divContent.attr("qtest.id", element.id);
            divContent.attr("qtest.type", element.type);

            divMainItem.append(divContent);

//            var a = $j("<a></a>");
//            a.attr("href", element.web_url);
//            a.attr("target", "_blank");
            ul.append(li.append(divItemContainer));
        });
        var mainDiv = $j("<div></div>");
        mainDiv.append(ul);
        jItem.append(mainDiv);
    }

}
function loadContainers(jParentNode, data) {
    var releases = data.releases || [];
    var testCycles = data.testCycles || [];
    var testSuites = data.testSuites || [];
    releases.forEach(function (e) {
        e.type = 'release';
    });

    testCycles.forEach(function (e) {
        e.type = 'test-cycle';
    });

    testSuites.forEach(function (e) {
        e.type = 'test-suite';
    });

    var items = releases;
    items = items.concat(testCycles);
    items = items.concat(testSuites);

    buildTree(jParentNode, items);

}