pub fn main() {
    if std::env::var("TARGET").unwrap() == "i686-win7-windows-gnu" {
        println!(
            "cargo:rustc-link-search=native={}/../stub/",
            std::env::var("CARGO_MANIFEST_DIR").unwrap()
        );
    }
}
