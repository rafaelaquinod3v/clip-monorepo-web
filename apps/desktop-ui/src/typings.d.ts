
interface Window {
  electronAPI: {
    sendData: (channel: string, data: any) => void;
    receiveData: (channel: string, func: (...args: any[]) => void) => void;
    invokeAction: (channel: string, data: any) => Promise<any>;
  };
}
