use anstyle::{AnsiColor, Color, Style};
use errcode::prelude::Result;
use log::{Level, LevelFilter, Log, Metadata, Record};
use std::{
    fmt::{Arguments, Display, Formatter},
    fs::{File, OpenOptions},
    io::{BufWriter, Write},
    sync::Mutex,
    time::Instant,
};

#[derive(Debug, Copy, Clone)]
pub struct LoggerOptions {
    pub log_file_name: &'static str,
    pub log_append: bool,
    pub log_level: LevelFilter,
}

pub fn register_logger(options: LoggerOptions) {
    if let Err(e) = register_logger_(options) {
        logging_fatal(&format!("Could not initialize logging: {e}"));
    }
}

fn register_logger_(options: LoggerOptions) -> Result<()> {
    let file = {
        let mut path = std::env::current_exe()?.canonicalize()?;
        path.pop();
        path.push(options.log_file_name);

        let file = if options.log_append {
            OpenOptions::new().append(true).create(true).open(path)?
        } else {
            OpenOptions::new().create(true).open(path)?
        };
        BufWriter::new(file)
    };

    log::set_max_level(options.log_level);
    log::set_boxed_logger(Box::new(LogHandler {
        log_start: Instant::now(),
        log_file: Mutex::new(file),
        log_level: options.log_level,
    }))?;
    Ok(())
}

fn logging_fatal(str: &str) -> ! {
    println!("Fatal error: {str}");
    std::process::exit(0xDEAD)
}

struct LogHandler {
    log_start: Instant,
    log_file: Mutex<BufWriter<File>>,
    log_level: LevelFilter,
}
impl LogHandler {
    fn record_log(&self, record: &mut LogRecord<'_>) {
        record.is_terminal = true;
        println!("{record}");

        record.is_terminal = false;
        let mut out_file = match self.log_file.lock() {
            Ok(o) => o,
            Err(_) => logging_fatal("Could not lock log mutex"),
        };
        if let Err(e) = writeln!(out_file, "{record}") {
            logging_fatal(&format!("Could not write log: {e}"))
        }
    }
}
impl Log for LogHandler {
    fn enabled(&self, metadata: &Metadata) -> bool {
        metadata.level().to_level_filter() <= self.log_level
    }
    fn log(&self, record: &Record) {
        let ts = Instant::now().duration_since(self.log_start).as_secs_f64();
        let mut record = if record.target() == "<lua>" {
            LogRecord {
                ts,
                level: record.level(),
                module: None,
                args: record.args(),
                is_terminal: true,
            }
        } else {
            LogRecord {
                ts,
                level: record.level(),
                module: record.module_path(),
                args: record.args(),
                is_terminal: true,
            }
        };
        self.record_log(&mut record);
    }
    fn flush(&self) {}
}

fn level_str(level: Level) -> &'static str {
    match level {
        Level::Error => "ERROR",
        Level::Warn => "WARN",
        Level::Info => "INFO",
        Level::Debug => "DEBUG",
        Level::Trace => "TRACE",
    }
}

fn level_ansi(level: Level) -> Style {
    match level {
        Level::Error => Style::new()
            .fg_color(Some(Color::Ansi(AnsiColor::BrightRed)))
            .bold(),
        Level::Warn => Style::new()
            .fg_color(Some(Color::Ansi(AnsiColor::BrightYellow)))
            .bold(),
        Level::Info => Style::new().fg_color(Some(Color::Ansi(AnsiColor::BrightBlue))),
        Level::Debug => Style::new().fg_color(Some(Color::Ansi(AnsiColor::Cyan))),
        Level::Trace => Style::new().fg_color(Some(Color::Ansi(AnsiColor::Blue))),
    }
}

struct LogRecord<'a> {
    ts: f64,
    level: Level,
    module: Option<&'a str>,
    args: &'a Arguments<'a>,
    is_terminal: bool,
}
impl<'a> Display for LogRecord<'a> {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        if self.is_terminal {
            let c_tm = Style::new().fg_color(Some(Color::Ansi(AnsiColor::BrightBlack)));
            let c_lv = level_ansi(self.level);
            write!(f, "{c_tm}{:.2}{c_tm:#} {c_lv}[{}]{c_lv:#} ", self.ts, level_str(self.level))?;
        } else {
            write!(f, "{:.2} [{}] ", self.ts, level_str(self.level))?;
        }
        if let Some(module) = self.module {
            write!(f, "{module}: ")?;
        }
        write!(f, "{}", self.args)?;
        Ok(())
    }
}
