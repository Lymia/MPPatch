#![allow(non_snake_case)]

use crate::{
    context,
    context::MppatchFeature,
    patcher::PatcherContext,
    patches::{netpatch, netpatch::OverrideType},
};
use errcode::prelude::Result;
use libc::c_int;
use log::{debug, trace};
use mlua::{
    ffi::{lua_CFunction, lua_getglobal},
    lua_State,
    prelude::{LuaFunction, LuaString},
    Lua, MultiValue, Table,
};
use mpp_common::logging::check_error;
use std::ffi::c_char;

make_func_overwrite! {
    f0, cvLuaPathLibrary_Register, cvLuaPathLibrary_Register_Hook,
    sym_cvLuaPathLibrary_Register, None,
    (posix_repr: "C-unwind", win_repr: "C-unwind"),
    (lua_c: *mut lua_State, lua_thread_c: *mut lua_State) -> c_int
}

make_func_proxy! {
    f1, cvLuaUtils_ExposeLibraryToThread, sym_cvLuaUtils_ExposeLibraryToThread,
    (posix_repr: "C-unwind", win_repr: "C-unwind"),
    (
        lua_c: *mut lua_State,
        lua_thread_c: *mut lua_State,
        lib_name: *const c_char,
        callback: lua_CFunction,
    ) -> c_int
}

unsafe fn cvLuaPathLibrary_Register_Hook(
    lua_c: *mut lua_State,
    lua_thread_c: *mut lua_State,
) -> c_int {
    unsafe {
        let ret = cvLuaPathLibrary_Register(lua_c, lua_thread_c);
        cvLuaUtils_ExposeLibraryToThread(
            lua_c,
            lua_thread_c,
            "_MPPatch_Core\0".as_ptr() as *const c_char,
            install_mppatch_table,
        );
        ret
    }
}

unsafe extern "C-unwind" fn install_mppatch_table(lua_c: *mut lua_State) -> c_int {
    check_error(install_mppatch_table_impl(lua_c));
    lua_getglobal(lua_c, "_MPPatch_Core_\0".as_ptr() as *const c_char);
    1
}

unsafe fn install_mppatch_table_impl(lua_c: *mut lua_State) -> Result<()> {
    unsafe {
        let lua = Lua::get_or_init_from_ptr(lua_c);
        lua.globals()
            .set("_MPPatch_Core_", create_mppatch_table(lua)?)?;
        Ok(())
    }
}

unsafe fn create_mppatch_table(lua: &Lua) -> Result<Table> {
    trace!("Building MPPatch function table...");
    let ctx = context::get_ctx();

    let patch_table = lua.create_table()?;
    patch_table.set("__mppatch_marker", 1)?;

    // misc functions
    patch_table.set(
        "debugPrint",
        lua.create_function(|_, value: LuaString| {
            debug!(target: "<lua>", "{}", value.to_string_lossy());
            Ok(())
        })?,
    )?;
    patch_table.set("getGlobals", lua.create_function(|lua, _: ()| Ok(lua.globals()))?)?;

    // shared table
    patch_table.set("shared", lua.create_table()?)?;

    // version table
    {
        let version_table = lua.create_table()?;
        version_table.set("versionString", ctx.version())?;
        version_table.set("platform", ctx.version_info.platform.name())?;
        version_table.set("sha256", ctx.sha256())?;
        version_table.set("buildId", ctx.build_id())?;
        patch_table.set("version", lock_table(lua, &version_table)?)?;
    }

    // config table
    {
        let config_table = lua.create_table()?;
        config_table.set("enableLogging", ctx.has_feature(MppatchFeature::Logging))?;
        config_table.set("enableDebug", ctx.has_feature(MppatchFeature::Debug))?;
        config_table.set("enableMultiplayerPatch", ctx.has_feature(MppatchFeature::Multiplayer))?;
        config_table.set("enableLuaJIT", ctx.has_feature(MppatchFeature::LuaJit))?;
        patch_table.set("config", lock_table(lua, &config_table)?)?;
    }

    // get actual globals table
    // in this context, this is the actual globals table
    let globals = lua.globals();

    // globals table
    {
        let globals_table = lua.create_table()?;
        globals_table.set("rawget", globals.get::<LuaFunction>("rawget")?)?;
        globals_table.set("rawset", globals.get::<LuaFunction>("rawset")?)?;
        patch_table.set("globals", lock_table(lua, &globals_table)?)?;
    }

    // NetPatch table
    if ctx.has_feature(MppatchFeature::Multiplayer) {
        let net_patch_table = lua.create_table()?;

        net_patch_table.set(
            "pushMod",
            lua.create_function(|_, (name, ver): (String, i32)| {
                netpatch::add_mod(&name, ver);
                Ok(())
            })?,
        )?;
        net_patch_table.set(
            "pushDLC",
            lua.create_function(|_, guid: String| {
                netpatch::add_dlc(&guid);
                Ok(())
            })?,
        )?;

        net_patch_table.set(
            "overrideReloadMods",
            lua.create_function(|_, _: ()| {
                netpatch::add_override(OverrideType::ForceReloadMods);
                Ok(())
            })?,
        )?;
        net_patch_table.set(
            "overrideReloadDLC",
            lua.create_function(|_, _: ()| {
                netpatch::add_override(OverrideType::ForceReloadDlcs);
                Ok(())
            })?,
        )?;
        net_patch_table.set(
            "overrideModList",
            lua.create_function(|_, _: ()| {
                netpatch::add_override(OverrideType::OverrideMods);
                Ok(())
            })?,
        )?;
        net_patch_table.set(
            "overrideDLCList",
            lua.create_function(|_, _: ()| {
                netpatch::add_override(OverrideType::OverrideDlcs);
                Ok(())
            })?,
        )?;
        net_patch_table.set(
            "reset",
            lua.create_function(|_, _: ()| {
                netpatch::reset();
                Ok(())
            })?,
        )?;
        patch_table.set("NetPatch", lock_table(lua, &net_patch_table)?)?;
    }

    Ok(patch_table)
}

fn lock_table(lua: &Lua, table: &Table) -> Result<Table> {
    let new_table = lua.create_table()?;
    let metatable = lua.create_table()?;

    metatable.set("__metatable", false)?;
    metatable.set("__index", table)?;
    metatable.set(
        "__newindex",
        lua.create_function(|_, _: MultiValue| -> mlua::Result<()> {
            Err(mlua::error::Error::runtime("Attempt to modify read-only table"))
        })?,
    )?;

    new_table.set_metatable(Some(metatable))?;
    Ok(new_table)
}
