use crate::patch_archive::{
    BuildMetadata,
    model::{
        ArchiveFileInfo, ArchiveIndex, ArchiveMetadataSection, DataSlice, MAGIC_HEADER,
        MAJOR_VERSION, MINOR_VERSION,
    },
};
use errcode::prelude::*;
use log::{debug, info, trace};
use rkyv::rancor;
use std::{collections::HashMap, io::Cursor};

#[derive(Debug, Clone)]
pub struct WriterOptions {
    pub enable_compression: bool,
    pub solid_directory_prefixes: Vec<(String, usize)>,
    pub num_prefixes: usize,
    pub metadata: BuildMetadata,
}

struct RawWriter {
    enable_compression: bool,
    buffer: Vec<u8>,
}
impl RawWriter {
    fn make_slice(&self, len: usize) -> Result<DataSlice> {
        let offset: u32 = self.buffer.len().try_into()?;
        let len: u32 = len
            .try_into()
            .convert(error_info!("Patch container format is 32-bit, this is WAY too much data!"))?;
        ensure!(offset.checked_add(len).is_some());
        Ok(DataSlice { offset, len })
    }

    fn write_raw_block(&mut self, data: &[u8], is_compressed: bool) -> Result<DataSlice> {
        let slice = self.make_slice(1 + data.len())?;
        self.buffer.push(is_compressed as u8);
        self.buffer.extend_from_slice(data);
        Ok(slice)
    }

    fn write_data(&mut self, data: &[u8]) -> Result<DataSlice> {
        if self.enable_compression {
            let compressed = zstd::compress_sized(Cursor::new(data), 15, data.len() as u64)?;
            if compressed.len() < data.len() {
                self.write_raw_block(&compressed, true)
            } else {
                self.write_raw_block(data, false)
            }
        } else {
            self.write_raw_block(data, false)
        }
    }

    fn align(&mut self, align_to: usize) {
        while self.buffer.len() % align_to != 0 {
            self.buffer.push(0);
        }
    }

    fn write_struct(&mut self, align_to: usize, data: &[u8]) -> Result<DataSlice> {
        self.align(align_to);
        let slice = self.make_slice(data.len())?;
        self.buffer.extend_from_slice(data);
        Ok(slice)
    }
}

struct SolidPrefixBuilder {
    /// yes this is a scary type signature
    /// outer vec: list of directory prefixes, corresponds to `solid_directory_prefixes`.
    /// middle vec: list of files in the particular prefix
    /// inner vec: buffer
    prefixes: Vec<Vec<Vec<u8>>>,
}
impl SolidPrefixBuilder {
    fn write_compressed_blocks(self, target: &mut RawWriter) -> Result<Vec<DataSlice>> {
        let mut blocks = Vec::new();
        let mut offset_table = Vec::with_capacity(1024);
        let mut buffer = Vec::with_capacity(1024 * 1024); // 1 MiB
        for files in self.prefixes {
            if files.is_empty() {
                // len = 0 is not otherwise allowed, so this is fine.
                blocks.push(DataSlice { offset: 0, len: 0 });
            } else {
                // clear both tables for the current round
                offset_table.clear();
                buffer.clear();

                // build the offset table
                {
                    let mut offset: u32 = (4 + 4 * files.len()).try_into()?;
                    for file in &files {
                        offset_table.push(offset);
                        offset = offset
                            .checked_add(file.len().try_into()?)
                            .convert(error_info!("Overflow while building offset table!"))?;
                    }
                }

                // prepare the offset table into the buffer
                buffer.extend_from_slice(&(files.len() as u32).to_le_bytes());
                for offset in &offset_table {
                    buffer.extend_from_slice(&offset.to_le_bytes());
                }

                // prepare the individual files into the buffer
                for file in files {
                    buffer.extend_from_slice(&file);
                }

                // compress and write to the internal buffer
                blocks.push(target.write_data(&buffer)?);
            }
        }
        Ok(blocks)
    }
}

pub fn encode_archive(
    options: WriterOptions,
    mut files: Vec<(String, Vec<u8>)>,
) -> Result<Vec<u8>> {
    info!("Encoding patch archive with {} files.", files.len());
    trace!("Options: {options:?}");

    ensure!(u32::try_from(files.len()).is_ok());
    ensure!(u32::try_from(options.num_prefixes).is_ok());
    files.sort_by_cached_key(|x| x.0.clone());

    let mut writer = RawWriter {
        enable_compression: options.enable_compression,
        buffer: Vec::with_capacity(1024 * 1024),
    };
    let mut prefixes =
        SolidPrefixBuilder { prefixes: vec![Vec::new(); options.solid_directory_prefixes.len()] };
    let mut index = ArchiveIndex { files: HashMap::new(), compressed_blocks: vec![] };

    'outer: for (name, data) in files {
        ensure!(!index.files.contains_key(&name));
        for (prefix, prefix_idx) in &options.solid_directory_prefixes {
            if name.starts_with(prefix) {
                debug!("Adding {name} to solid archive #{prefix_idx}... ({} bytes)", data.len());
                let file_idx = prefixes.prefixes[*prefix_idx].len() as u32;
                prefixes.prefixes[*prefix_idx].push(data);
                index
                    .files
                    .insert(name, ArchiveFileInfo::CompressedBlock(*prefix_idx as u32, file_idx));
                continue 'outer;
            }
        }

        debug!("Writing {name}... ({} bytes)", data.len());
        let block = writer.write_data(&data)?;
        index.files.insert(name, ArchiveFileInfo::Data(block));
    }

    debug!("Processing block compressed directories...");
    index.compressed_blocks = prefixes.write_compressed_blocks(&mut writer)?;

    debug!("Writing index...");
    let index = writer.write_struct(4, rkyv::to_bytes::<rancor::Error>(&index)?.as_slice())?;
    let signature = ArchiveMetadataSection {
        signature: String::from("MPPatch"),
        manifest_version: 1,
        metadata: options.metadata,
        archive_info: index,
    };
    let metadata_index =
        writer.write_struct(4, rkyv::to_bytes::<rancor::Error>(&signature)?.as_slice())?;

    debug!("Writing header...");
    writer.align(4);
    let start_len: u32 = writer
        .buffer
        .len()
        .try_into()
        .convert(error_info!("Final archive too long."))?;
    {
        let b = &mut writer.buffer;
        b.extend_from_slice(&MAGIC_HEADER);
        b.extend_from_slice(&MAJOR_VERSION.to_le_bytes());
        b.extend_from_slice(&MINOR_VERSION.to_le_bytes());
        b.extend_from_slice(&start_len.to_le_bytes());
        b.extend_from_slice(&metadata_index.offset.to_le_bytes());
        b.extend_from_slice(&metadata_index.len.to_le_bytes());
    }

    Ok(writer.buffer)
}
