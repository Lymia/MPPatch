/*
 * Copyright (c) 2015-2023 Lymia Kanokawa <lymia@lymia.moe>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

#![feature(c_unwind)]

use crate::rt_init::MppatchFeature;
use ctor::ctor;

mod hook_lua;
mod hook_luajit;
mod hook_netpatch;
mod rt_cpplist;
mod rt_init;
mod rt_patch;
mod rt_platform;
mod versions;

fn ctor_impl() -> anyhow::Result<()> {
    let ctx = rt_init::run()?;
    if ctx.has_feature(MppatchFeature::LuaJit) {
        hook_luajit::init(&ctx)?;
    }
    if ctx.has_feature(MppatchFeature::Multiplayer) {
        hook_lua::init(&ctx)?;
        hook_netpatch::init(&ctx)?;
    }
    Ok(())
}

#[ctor]
fn ctor() {
    ctor_impl().unwrap();
}
