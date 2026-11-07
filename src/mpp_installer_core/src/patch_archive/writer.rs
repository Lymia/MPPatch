use crate::patch_archive::{BuildMetadata, model::ArchiveMetadataSection};
use errcode::prelude::*;
use log::trace;
use rkyv::rancor;

#[derive(Debug, Clone)]
pub struct WriterOptions {
    pub enable_compression: bool,
    pub solid_directory_prefixes: Vec<String>,
    pub metadata: BuildMetadata,
}

pub fn encode_archive(options: WriterOptions, files: Vec<(String, Vec<u8>)>) -> Result<Vec<u8>> {
    trace!("Encoding patch archive with {} files.", files.len());
    trace!("Options: {options:#?}");

    let signature = ArchiveMetadataSection {
        signature: String::from("MPPatch"),
        major_version: 1,
        minor_version: 0,
        metadata: options.metadata,
    };
    Ok(rkyv::to_bytes::<rancor::Error>(&signature)?.to_vec())
}
