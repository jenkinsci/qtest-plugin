var allSelectElements = document.getElementsBySelector('.setting-main .setting-input.dropdownList');
var qTestPluginName = 'submitJUnitTestResultsToqTest: Submit jUnit test result to qTest';
var currentSelect;
for (var i = 0; i < allSelectElements.length; i++) {
  var options = allSelectElements[i].options;
  for (var j = 0; j < options.length; j++) {
    if (options[j].value == qTestPluginName) {
      currentSelect = allSelectElements[i];
    }
  }
}
var selectOnchange = currentSelect.onchange;
currentSelect.onchange = function() {
  selectOnchange();
  if (currentSelect.value == qTestPluginName) {
    var configForm = document.getElementsBySelector('form[name=config]')[0];
    var radiosObject = configForm.radios;
    var objectKeys = Object.keys(radiosObject);
    for (var i = 0; i < objectKeys.length; i++) {
      radiosObject[objectKeys[i]].updateButtons();
    }
  } else {
    console.log('Current value: ' + currentSelect.value);
  }
}