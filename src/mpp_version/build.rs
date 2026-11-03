use serde::{Deserialize, Serialize};
use std::{collections::HashMap, fmt::Write, fs, path::PathBuf};

fn main() {
    println!("cargo::rerun-if-changed=noceg");
    let out_dir = std::env::var("OUT_DIR").unwrap();
    for path in fs::read_dir("noceg").unwrap() {
        let path = path.unwrap();
        let name = path.file_name();
        let name = name.to_string_lossy();
        if name.contains(".json") {
            let out_name = name.replace(".json", ".inc");
            let mut out_path = PathBuf::from(out_dir.clone());
            out_path.push(out_name);

            let json = fs::read_to_string(path.path()).unwrap();
            let info: CegInfo = serde_json::from_str(&json).unwrap();
            fs::write(out_path, generate_noceg_inc(&info)).unwrap();
        }
    }
}

#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "PascalCase")]
pub struct CegInfo {
    constant_or_stolen: Vec<HashMap<String, ConstantOrStolen>>,
    init: String,
    integrity: Vec<String>,
    terminate: String,
}

#[derive(Serialize, Deserialize, Debug)]
#[serde(rename_all = "PascalCase")]
pub struct ConstantOrStolen {
    is_address: bool,
    prologue: String,
    #[serde(rename = "Type")]
    ty: i32,
    value: String,
}

pub fn parse_hex_u32(s: &str) -> u32 {
    let s = s.trim_start_matches("0x");
    u32::from_str_radix(s, 16).unwrap()
}

pub fn generate_noceg_inc(data: &CegInfo) -> String {
    let mut out = String::new();

    let mut static_functions = Vec::new();
    let mut relative_functions = Vec::new();
    let mut stub_functions = Vec::new();

    for func in &data.constant_or_stolen {
        assert_eq!(func.len(), 1);
        let (key, value) = func.iter().next().unwrap();
        assert_eq!(key, &value.prologue);
        assert!(value.ty == 2 || value.ty == 3);

        let tuple = (parse_hex_u32(&value.prologue), parse_hex_u32(&value.value));
        if value.is_address {
            relative_functions.push(tuple);
        } else {
            static_functions.push(tuple);
        }
    }

    stub_functions.push(parse_hex_u32(&data.init));
    for integrity in &data.integrity {
        stub_functions.push(parse_hex_u32(&integrity));
    }
    stub_functions.push(parse_hex_u32(&data.terminate));

    static_functions.sort();
    relative_functions.sort();
    stub_functions.sort();

    out.push_str("CegPatches {\n");
    out.push_str("  static_functions: &[\n");
    for (off, value) in static_functions {
        writeln!(out, "    (Offset(0x{off:x}), 0x{value:x}),").unwrap();
    }
    out.push_str("  ],\n");
    out.push_str("  relative_functions: &[\n");
    for (off, value) in relative_functions {
        writeln!(out, "    (Offset(0x{off:x}), Offset(0x{value:x})),").unwrap();
    }
    out.push_str("  ],\n");
    out.push_str("  stub_functions: &[\n");
    for off in stub_functions {
        writeln!(out, "    Offset(0x{off:x}),").unwrap();
    }
    out.push_str("  ],\n");
    out.push_str("}\n");
    out
}
