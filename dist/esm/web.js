var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { WebPlugin, } from '@capacitor/core';
export class PersistentNotificationWeb extends WebPlugin {
    constructor() {
        super({
            name: 'PersistentNotification',
            platforms: ['web']
        });
    }
    open(options) {
        return __awaiter(this, void 0, void 0, function* () {
            console.warn("No operation on web platform.", options);
            return false;
        });
    }
    update(options) {
        return __awaiter(this, void 0, void 0, function* () {
            console.warn("No operation on web platform.", options);
            return false;
        });
    }
    close() {
        return __awaiter(this, void 0, void 0, function* () {
            console.warn("No operation on web platform.");
            return false;
        });
    }
    getState() {
        return __awaiter(this, void 0, void 0, function* () {
            console.warn("No operation on web platform.");
            return { isOpen: false };
        });
    }
    appToForeground() {
        return __awaiter(this, void 0, void 0, function* () {
            console.warn("No operation on web platform.");
            return false;
        });
    }
}
const PersistentNotification = new PersistentNotificationWeb();
export { PersistentNotification };
import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(PersistentNotification);
//# sourceMappingURL=web.js.map