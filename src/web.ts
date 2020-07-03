import { WebPlugin, } from '@capacitor/core';
import { PersistentNotificationPlugin } from './definitions';

interface PersistentNotificationState {
    isOpen: boolean;
}

interface PersistentNotificationOptions
{
    title: string, 
    icon: string, 
    body: string, 
    actions: Array<NotificationAction>, 
    color: string, 
    badge: string
}

export class PersistentNotificationWeb extends WebPlugin implements PersistentNotificationPlugin {
    constructor() {
        super({
            name: 'PersistentNotification',
            platforms: ['web']
        });
    }

    async open(options: PersistentNotificationOptions): Promise<any> 
    {
        console.warn("No operation on web platform.", options);
        return false;
    }

    async update(options: PersistentNotificationOptions): Promise<any> 
    {
        console.warn("No operation on web platform.", options);
        return false;
    }

    async close(): Promise<any>
    {
        console.warn("No operation on web platform.");
        return false;
    }

    async getState(): Promise<PersistentNotificationState>
    {
        console.warn("No operation on web platform.");
        return {isOpen: false};
    }

    async appToForeground(): Promise<any>
    {
        console.warn("No operation on web platform.");
        return false;
    }
}

const PersistentNotification = new PersistentNotificationWeb();

export { PersistentNotification };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(PersistentNotification);
