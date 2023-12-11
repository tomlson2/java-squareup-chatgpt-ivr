/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cloud.cleo.squareup.enums;

import com.amazonaws.services.lambda.runtime.events.LexV2Event;
import com.amazonaws.services.lambda.runtime.events.LexV2Event.DialogAction;

/**
 *
 * @author sjensen
 */
public enum LexDialogAction {
    Delegate,
    ElicitIntent,
    ElicitSlot,
    ConfirmIntent,
    Close;
    
    
    public DialogAction getDialogAction() {
        return LexV2Event.DialogAction.builder().withType(this.name()).build();
    }
}
