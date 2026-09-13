#!/usr/bin/env python3
"""Generate the Tweakeroo config/group inventory used by halfmasa."""

from __future__ import annotations

import json
import re
import zipfile
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
TWEAKEROO_JAR = Path(
    r"D:\我的世界\.minecraft\versions\26.2-Fabric_0.19.3\mods\tweakeroo-fabric-26.2-0.29.2.jar"
)
PROVIDER = ROOT / "src/main/java/io/github/halfmasa/xaerobinding/gui/TweakerooConfigExpansionProvider.java"
ZH_LANG = ROOT / "src/main/resources/assets/halfmasa/lang/zh_cn.json"
EN_LANG = ROOT / "src/main/resources/assets/halfmasa/lang/en_us.json"
OUTPUT = ROOT / "docs/tweakeroo-0.29.2-config-groups-zh.txt"

CATEGORIES = (
    ("feature_toggle", "Feature Toggles", "功能开关"),
    ("generic", "Generic", "通用"),
    ("lists", "Lists", "列表"),
    ("hotkey", "Hotkeys", "快捷键"),
    ("fixes", "Fixes", "修复"),
    ("disable", "Disable", "禁用"),
)


@dataclass(frozen=True)
class ConfigRecord:
    category_id: str
    category_en: str
    category_zh: str
    name: str
    english_name: str
    chinese_name: str


def load_json(path: Path) -> dict[str, str]:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def load_jar_lang(language: str) -> dict[str, str]:
    with zipfile.ZipFile(TWEAKEROO_JAR) as archive:
        with archive.open(f"assets/tweakeroo/lang/{language}.json") as handle:
            return json.load(handle)


def actual_config_name(language_key_name: str) -> str:
    if language_key_name == "tweakSneak_1_15_2":
        return "tweakSneak_1.15.2"
    return language_key_name


def collect_records(zh: dict[str, str], en: dict[str, str]) -> list[ConfigRecord]:
    records: list[ConfigRecord] = []
    for category_id, category_en, category_zh in CATEGORIES:
        prefix = f"tweakeroo.config.{category_id}.name."
        for key, chinese_name in zh.items():
            if not key.startswith(prefix):
                continue
            language_key_name = key[len(prefix) :]
            records.append(
                ConfigRecord(
                    category_id=category_id,
                    category_en=category_en,
                    category_zh=category_zh,
                    name=actual_config_name(language_key_name),
                    english_name=en.get(key, "(missing English translation)"),
                    chinese_name=chinese_name.replace("\n", " / "),
                )
            )
    return records


def collect_groups(source: str) -> tuple[list[tuple[str, list[str]]], dict[str, str]]:
    primary_block = source.split("private static final Map<String, String> PRIMARY_CONFIGS", 1)[1]
    primary_block = primary_block.split("private static final List<GroupDefinition> GROUPS", 1)[0]
    primary = dict(re.findall(r'Map\.entry\("([^"]+)", "([^"]+)"\)', primary_block))

    group_block = source.split("private static final List<GroupDefinition> GROUPS = List.of(", 1)[1]
    group_block = group_block.split("private final Map<IConfigBase", 1)[0]
    starts = list(re.finditer(r'group\("([^"]+)"', group_block))
    groups: list[tuple[str, list[str]]] = []
    for index, match in enumerate(starts):
        end = starts[index + 1].start() if index + 1 < len(starts) else len(group_block)
        body = group_block[match.end() : end]
        members = re.findall(r'"([^"]+)"', body)
        ordered: list[str] = []
        if match.group(1) in primary:
            ordered.append(primary[match.group(1)])
        for member in members:
            if member not in ordered:
                ordered.append(member)
        groups.append((match.group(1), ordered))
    return groups, primary


def clean_label(value: str) -> str:
    return value.replace("\n", " / ")


def main() -> None:
    jar_zh = load_jar_lang("zh_cn")
    jar_en = load_jar_lang("en_us")
    halfmasa_zh = load_json(ZH_LANG)
    halfmasa_en = load_json(EN_LANG)
    records = collect_records(jar_zh, jar_en)
    groups, primary = collect_groups(PROVIDER.read_text(encoding="utf-8"))

    group_by_config: dict[str, str] = {}
    members_by_group: dict[str, list[str]] = {}
    for group_id, members in groups:
        members_by_group[group_id] = members
        for name in members:
            group_by_config.setdefault(name, group_id)

    grouped_records: dict[str, list[ConfigRecord]] = defaultdict(list)
    ungrouped_records: dict[str, list[ConfigRecord]] = defaultdict(list)
    records_by_name: dict[str, list[ConfigRecord]] = defaultdict(list)
    for record in records:
        records_by_name[record.name].append(record)
        group_id = None if record.category_id == "disable" else group_by_config.get(record.name)
        if group_id is None:
            ungrouped_records[record.category_id].append(record)
        else:
            grouped_records[group_id].append(record)

    lines = [
        "全部配置与内置分组清单：Tweakeroo 0.29.2 / halfmasa",
        "适用版本：Minecraft 26.2 / Fabric / Tweakeroo 0.29.2",
        "说明：同一英文配置名若同时存在于多个原生分类，会按分类分别列出。",
        "说明：Disable（禁用）分类按设计保持独立，不参与折叠分组。",
        "",
        f"配置记录总数：{len(records)}",
        f"内置折叠分组数：{len(groups)}",
        f"已归组配置记录数：{sum(len(items) for items in grouped_records.values())}",
        f"未归组或保持独立记录数：{sum(len(items) for items in ungrouped_records.values())}",
        "",
        "==================== 内置折叠分组 ====================",
        "",
    ]

    category_order = {category_id: index for index, (category_id, _, _) in enumerate(CATEGORIES)}
    record_order = {id(record): index for index, record in enumerate(records)}

    for group_id, members in groups:
        group_zh_key = f"halfmasa.config.tweakeroo.name.{group_id}"
        group_en = clean_label(halfmasa_en.get(group_zh_key, group_id))
        group_zh = clean_label(halfmasa_zh.get(group_zh_key, group_id))
        main_config = primary.get(group_id, "无")
        main_record = next((record for record in records if record.name == main_config), None)
        main_zh = main_record.chinese_name if main_record else "无"
        lines.extend(
            [
                f"分组中文名：{group_zh} | 分组英文信息：ID={group_id}，名称={group_en}",
                f"主配置中文名：{main_zh} | 主配置英文信息：{main_config}",
                f"配置（中文功能名 | 原生分类 | 角色 | 英文配置名 | 英文名称）：",
            ]
        )
        member_order = {name: index for index, name in enumerate(members)}
        items = sorted(
            grouped_records.get(group_id, []),
            key=lambda record: (
                member_order.get(record.name, len(member_order)),
                category_order[record.category_id],
                record_order[id(record)],
            ),
        )
        for record in items:
            role = "主配置" if record.name == main_config else "子配置"
            lines.append(
                f"  {clean_label(record.chinese_name)} | 原生分类：{record.category_zh} | "
                f"角色：{role} | 英文配置名：{record.name} | 英文名称：{clean_label(record.english_name)}"
            )
        missing = [name for name in members if not any(item.name == name for item in items)]
        for name in missing:
            assigned_group = group_by_config.get(name)
            if records_by_name.get(name) and assigned_group != group_id:
                lines.append(
                    f"  分组定义重叠 | 中文名称：无 | 角色：实际归入 {assigned_group}（先定义优先） | "
                    f"英文配置名：{name}"
                )
            else:
                lines.append(
                    f"  当前版本未找到 | 中文名称：无 | 角色：仅存在于分组定义 | 英文配置名：{name}"
                )
        lines.append("")

    lines.extend(["==================== 未分组或保持独立 ====================", ""])
    for category_id, category_en, category_zh in CATEGORIES:
        items = ungrouped_records.get(category_id, [])
        if not items:
            continue
        lines.extend(
            [
                f"分类中文名：未分组 - {category_zh}",
                f"分类英文信息：ID=ungrouped_{category_id}，名称=Ungrouped - {category_en}",
                "配置（中文功能名 | 原生分类 | 英文配置名 | 英文名称）：",
            ]
        )
        for record in items:
            lines.append(
                f"  {clean_label(record.chinese_name)} | 原生分类：{record.category_zh} | "
                f"英文配置名：{record.name} | 英文名称：{clean_label(record.english_name)}"
            )
        lines.append("")

    OUTPUT.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8-sig")
    print(f"Wrote {OUTPUT}")
    print(f"records={len(records)} groups={len(groups)} grouped={sum(map(len, grouped_records.values()))} "
          f"ungrouped={sum(map(len, ungrouped_records.values()))}")


if __name__ == "__main__":
    main()
