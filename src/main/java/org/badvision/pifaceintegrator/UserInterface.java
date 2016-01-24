package org.badvision.pifaceintegrator;

import javafx.beans.property.BooleanProperty;

/**
 *
 * @author blurry
 */
public interface UserInterface {

    public void mainLoop();

    public static enum SystemStatus{
        active,scanning,error,inactive
    };
    
    public void setMeshStatus(SystemStatus status);
    public BooleanProperty meshConnectedProperty();
    public void showMeshBroadcast(String message);
    public void clearMeshVariables();
    public void updateMeshVariable(String name, String val);

    public void setPifaceStatus(SystemStatus status);
    public void setPifaceValue(int pin, int value);
    public void showPifaceBroadcast(String message);

    public void announceShutdown();
    public void addShutdownHook(Runnable listener);
}