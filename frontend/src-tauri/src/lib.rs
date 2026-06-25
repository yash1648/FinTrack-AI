use serde::Serialize;

/// System info response
#[derive(Debug, Serialize)]
pub struct SystemInfo {
    pub platform: String,
    pub version: String,
}

/// Tauri command: get application system information
#[tauri::command]
fn get_system_info() -> SystemInfo {
    SystemInfo {
        platform: std::env::consts::OS.to_string(),
        version: env!("CARGO_PKG_VERSION").to_string(),
    }
}

/// Tauri command: get the backend API base URL
/// Defaults to localhost:8080 which is the Spring Boot backend
#[tauri::command]
fn get_api_url() -> String {
    std::env::var("TAURI_API_URL").unwrap_or_else(|_| "http://localhost:8080".into())
}

/// Tauri command: get the WebSocket URL for real-time notifications
#[tauri::command]
fn get_ws_url() -> String {
    std::env::var("TAURI_WS_URL").unwrap_or_else(|_| "http://localhost:8080".into())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![
            get_system_info,
            get_api_url,
            get_ws_url,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
