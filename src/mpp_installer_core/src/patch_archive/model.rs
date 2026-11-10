use rkyv::{Archive, Deserialize, Serialize};
use std::collections::HashMap;

pub const MAGIC_HEADER: [u8; 12] = *b"\x89MppDatPk\x0D\x0A\x0A";

pub const MAJOR_VERSION: u16 = 1;
pub const MINOR_VERSION: u16 = 0;

#[derive(Debug, Clone)]
#[derive(Archive, Serialize, Deserialize)]
pub struct DataSlice {
    pub offset: u32,
    pub len: u32,
}

#[derive(Debug, Clone)]
#[derive(Archive, Serialize, Deserialize)]
pub struct ArchiveMetadataSection {
    pub signature: String,
    pub manifest_version: u32,
    pub metadata: BuildMetadata,
    pub archive_info: DataSlice,
}

#[derive(Debug, Clone)]
#[derive(Archive, Serialize, Deserialize)]
pub struct BuildMetadata {
    pub version: String,
    pub git_commit: Vec<u8>,
    pub git_tag: Option<String>,
    pub git_is_clean: bool,
    pub build_id: uuid::Bytes,
    pub build_time: u64,
    pub build_user: String,
    pub build_host: String,
    pub build_is_ci: bool,
}

#[derive(Debug, Clone)]
#[derive(Archive, Serialize, Deserialize)]
pub struct ArchiveIndex {
    pub files: HashMap<String, ArchiveFileInfo>,
    pub compressed_blocks: Vec<DataSlice>,
}

#[derive(Debug, Clone)]
#[derive(Archive, Serialize, Deserialize)]
pub enum ArchiveFileInfo {
    Data(DataSlice),
    CompressedBlock(u32, u32),
}

#[derive(Debug, Clone)]
#[derive(Archive, Serialize, Deserialize)]
pub struct CompressedBlockHeader {
    pub offsets: Vec<DataSlice>,
}
