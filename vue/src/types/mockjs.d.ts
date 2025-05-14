declare module 'mockjs' {
  interface MockOptions {
    url: string;
    type: string;
    body?: string;
  }

  interface MockSetupOptions {
    timeout?: string | number;
  }

  interface MockRandom {
    string: (min: number, max?: number) => string;
    number: (min: number, max?: number) => number;
    boolean: (min?: number, max?: number) => boolean;
    date: (format?: string) => string;
    image: (size?: string, background?: string, foreground?: string, format?: string, text?: string) => string;
    color: () => string;
    // 更多Random方法...
  }

  const Mock: {
    mock: <T>(url: string, type: string, template: object | ((options: MockOptions) => T)) => void;
    setup: (options: MockSetupOptions) => void;
    Random: MockRandom;
  };

  export default Mock;
}
