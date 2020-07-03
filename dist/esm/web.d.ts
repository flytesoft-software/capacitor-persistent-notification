import { WebPlugin } from '@capacitor/core';
import { PersistentNotificationPlugin } from './definitions';
interface PersistentNotificationState {
    isOpen: boolean;
}
interface PersistentNotificationOptions {
    title: string;
    icon: string;
    body: string;
    actions: Array<NotificationAction>;
    color: string;
    badge: string;
}
export declare class PersistentNotificationWeb extends WebPlugin implements PersistentNotificationPlugin {
    constructor();
    open(options: PersistentNotificationOptions): Promise<any>;
    update(options: PersistentNotificationOptions): Promise<any>;
    close(): Promise<any>;
    getState(): Promise<PersistentNotificationState>;
    appToForeground(): Promise<any>;
}
declare const PersistentNotification: PersistentNotificationWeb;
export { PersistentNotification };
