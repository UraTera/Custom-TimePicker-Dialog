## Example of creating a complex custom AlertDialog.

Using AlertDialog greatly simplifies the code compared to DialogFragment.

![TimeDialog](https://github.com/user-attachments/assets/e464a74f-ee20-4fc6-9aa1-33616c250957)

To use the ready-made library, add the dependency:
```
dependencies {

    implementation("io.github.uratera:time_dialog:1.0.1")
}
```
### Methods

|Methods              |Description           |Default value|
|---------------------|----------------------|-------------|
|setHour              |Value hour            |Current hour
|setMin               |Value minute          |Current minute
|setSec               |Value second          |Current second
|setFadingExtent      |Edges fading extent (0-10)|7
|setFontFamily                      |Text font             |default
|setHintTextHour      |Text hint hours       |"h"
|setHintTextMin       |Text hint minutes     |"m"
|setHintTextSec       |Text hint seconds     |"s"
|setHintColor         |Color text hint       |black
|setHintSize          |Size text hint        |16sp
|setIntervalLongPress |Interval update of long press |200
|setMaxHours          |Maximum value hours   |100
|setShowHint          |Show hint             |true
|setShowRows5         |Show 5 rows           |true
|setShowSec           |Show seconds picker   |true
|setTextColor         |Color unselected text |gray
|setTextColorSel      |Color selected text   |black
|setTextSize          |Size unselected text  |20sp
|setTextSizeSel       |Size selected text    |24sp
|setButtonCancelColor |Cancel button color   |white
|setButtonOkColor     |OK button color       |white
|setTextCancelColor   |Cancel button text color |dark blue
|setTextOkColor       |OK button text color  |dark blue


Usage

Kotlin
```
private fun openDialog(){
    val dialog = TimeDialog(this)
    dialog.hour = myHour
    dialog.min = myMin
    dialog.sec = mySec
    dialog.setOnChangeListener { hour, min, sec ->
        myHour = hour
        myMin = min
        mySec = sec
    }
}
```
 
Java
```
private void openDialog() {
    TimeDialog dialog = new TimeDialog(this);
    dialog.setHour(myHour);
    dialog.setMin(myMin);
    dialog.setSec(mySec);
    dialog.setOnChangeListener((integer, integer2, integer3) -> {
        myHour = integer;
        myMin = integer2;
        mySec = integer3;
        return null;
    });
}
```
