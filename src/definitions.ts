declare module "@capacitor/core" {
  interface PluginRegistry {
    PersistentNotification: PersistentNotificationPlugin;
  }
}

export interface PersistentNotificationPlugin {
    open(options: PersistentNotificationOptions): Promise<any>;
    update(options: PersistentNotificationOptions): Promise<any>;
    close(): Promise<any>;
    getState(): Promise<PersistentNotificationState>;
    appToForeground(): Promise<any>;
}

export interface PersistentNotificationOptions
{
    title: string, 
    icon: string, 
    body: string, 
    actions: Array<NotificationAction>, 
    color: string, 
    badge: string
}

export interface PersistentNotificationState {
    isOpen: boolean;
}
