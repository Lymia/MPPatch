use errcode::prelude::*;
use git2::{DescribeOptions, Repository, RepositoryState, StatusOptions};
use mpp_installer_core::patch_archive::{BuildMetadata, WriterOptions};
use serde::Deserialize;
use std::time::SystemTime;

#[derive(Deserialize)]
struct CargoManifest {
    package: CargoManifestPackage,
}

#[derive(Deserialize)]
struct CargoManifestPackage {
    version: String,
}

fn build_metadata() -> Result<BuildMetadata> {
    let manifest = std::fs::read_to_string(concat!(
        env!("CARGO_WORKSPACE_DIR"),
        "/src/mpp_hooklib/Cargo.toml"
    ))?;
    let manifest = toml::from_str::<CargoManifest>(&manifest)?;

    let repo = Repository::open(env!("CARGO_WORKSPACE_DIR"))?;
    Ok(BuildMetadata {
        version: manifest.package.version,
        git_commit: repo.head()?.peel_to_commit()?.id().as_bytes().to_vec(),
        git_tag: repo
            .describe(
                DescribeOptions::new()
                    .describe_tags()
                    .max_candidates_tags(0)
                    .pattern("v*"),
            )
            .and_then(|x| x.format(None))
            .ok(),
        git_is_clean: repo.state() == RepositoryState::Clean
            && repo
                .statuses(Some(StatusOptions::new().include_untracked(true)))?
                .is_empty(),
        build_id: *uuid::Uuid::new_v4().as_bytes(),
        build_time: SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)?
            .as_secs(),
        build_user: whoami::username()?,
        build_host: whoami::hostname()?,
        build_is_ci: std::env::var_os("GITHUB_ACTIONS").is_some(),
    })
}

fn find_files(prefix: &str) -> Result<Vec<(String, Vec<u8>)>> {
    let mut files = Vec::new();
    for file in glob::glob(&format!("{prefix}/**/*"))? {
        let file = file?;
        if file.is_file() {
            let name = file.to_string_lossy().to_string();
            let name = name[prefix.len() + 1..].to_string();
            files.push((name, std::fs::read(file)?));
        }
    }
    Ok(files)
}

pub fn build_patch() -> Result<Vec<u8>> {
    let metadata = build_metadata()?;
    let data = mpp_installer_core::patch_archive::encode_archive(
        WriterOptions {
            enable_compression: true,
            solid_directory_prefixes: vec![(String::from("ui/"), 0), (String::from("meta/"), 0)],
            num_prefixes: 1,
            metadata,
        },
        find_files("src/patch_data")?,
    )?;
    Ok(data)
}

pub fn test() {
    let data = build_patch().unwrap();
    std::fs::write("test.bin", data).unwrap();
}
