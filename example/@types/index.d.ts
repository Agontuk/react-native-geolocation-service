declare module '@voximplant/react-native-foreground-service' {
  export default class VIForegroundService {
    static getInstance(): VIForegroundService;

    async createNotificationChannel(channelConfig: any);

    async startService(notificationConfig: any);

    async stopService();
  }
}
