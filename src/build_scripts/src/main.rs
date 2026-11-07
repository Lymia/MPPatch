use log::LevelFilter;
use mpp_common::logging::LoggerOptions;

mod build_patch;

fn main() {
    mpp_common::logging::register_logger(LoggerOptions {
        log_file_name: None,
        log_append: false,
        log_level: LevelFilter::Trace,
    });
    build_patch::test();
}
