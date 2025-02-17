package org.anti_ad.mc.ipnext.config

import org.anti_ad.mc.common.vanilla.alias.glue.I18n



private const val ENUM = "inventoryprofiles.enum"

enum class SortingMethod(val ruleName: String?) {
    DEFAULT("default"),
    ITEM_NAME("item_name"),
    ITEM_ID("item_id"),
    RAW_ID("raw_id"),
    CUSTOM(null);

    override fun toString(): String =
        I18n.translate("$ENUM.sorting_method.${name.lowercase()}")
}

enum class SortingMethodIndividual {
    GLOBAL,
    DEFAULT,
    ITEM_NAME,
    ITEM_ID,
    RAW_ID,
    CUSTOM;

    override fun toString(): String =
        if (this == GLOBAL)
            I18n.translate(
                "$ENUM.sorting_method.global",
                ModSettings.SORT_ORDER.value.toString().substringBefore('(').trim()
            )
        else
            I18n.translate("$ENUM.sorting_method.${name.lowercase()}")
}

enum class PostAction {
    NONE,
    GROUP_IN_ROWS,
    GROUP_IN_COLUMNS,
    DISTRIBUTE_EVENLY,
    SHUFFLE,
    FILL_ONE,
    REVERSE;

    override fun toString(): String =
        I18n.translate("$ENUM.post_action.${name.lowercase()}")
}

enum class ThresholdUnit {
    ABSOLUTE,
    PERCENTAGE;

    override fun toString(): String =
        I18n.translate("$ENUM.threshold_unit.${name.lowercase()}")
}

enum class ContinuousCraftingCheckboxValue {
    REMEMBER,
    CHECKED,
    UNCHECKED;

    override fun toString(): String =
        I18n.translate("$ENUM.continuous_crafting_checkbox_value.${name.lowercase()}")
}

enum class SwitchType {
    TOGGLE,
    HOLD;

    override fun toString(): String =
        I18n.translate("$ENUM.switch_type.${name.lowercase()}")
}

enum class DiffCalculatorType {
    SIMPLE,
    SCORE_BASED_SINGLE,
    SCORE_BASED_DUAL,
    ;

//  override fun toString(): String =
//    I18n.translate("$ENUM.diff_calculator_type.${name.toLowerCase()}")
}
