package org.gnu.itsmoroto.midandpad

data class PresetItem(val id: Long, val name: String){
    override fun toString(): String {
        return name
    }
}
