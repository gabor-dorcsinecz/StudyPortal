package code.model

import org.tresto.traits.LogHelper

object InitDatabase extends LogHelper{

  def initDefaults() {
    val allUsers = MapUser.count() //The cache is TOTALLY empty at start
    if (allUsers == 0) {
      return
    }

    log.debug("============================== S T A R T   D B   I N I T   ==============================")
    MapUser.create.email("gaborvisor").password("123123123").userType(MapUser.USERTYPE_SITEVISOR).validated(true).save
  }
}