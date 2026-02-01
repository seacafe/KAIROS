/// <reference types="vite/client" />

// CSS 모듈 타입 선언
declare module '*.css' {
    const content: { [className: string]: string };
    export default content;
}

// 이미지 타입 선언
declare module '*.svg' {
    const content: string;
    export default content;
}

declare module '*.png' {
    const content: string;
    export default content;
}

declare module '*.jpg' {
    const content: string;
    export default content;
}
