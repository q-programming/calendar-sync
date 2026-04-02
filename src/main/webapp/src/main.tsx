import { createRoot } from "react-dom/client";
import { Provider } from "react-redux";
import { store } from "@/store/store";
import { setupInterceptors } from "@/store/setupInterceptors";
import App from "./App";
import "./index.css";

setupInterceptors(store.dispatch);

createRoot(document.getElementById("root")!).render(
  <Provider store={store}>
    <App />
  </Provider>
);
