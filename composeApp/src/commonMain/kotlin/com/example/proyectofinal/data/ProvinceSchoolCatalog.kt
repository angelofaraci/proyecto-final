package com.example.proyectofinal.data

import com.example.proyectofinal.domain.StudentTrack

data class SchoolYearOption(
    val label: String,
    val schoolYear: Int,
    val allowedTracks: Set<StudentTrack>
)

data class ProvinceSchoolConfiguration(
    val province: String,
    val primaryYearCount: Int
) {
    init {
        require(primaryYearCount == 6 || primaryYearCount == 7) {
            "Only 6-year and 7-year primary structures are supported."
        }
    }

    val primaryYears: IntRange = 1..primaryYearCount
    val secondaryYears: IntRange = (primaryYearCount + 1)..12
    val technicalSecondaryYears: IntRange = (primaryYearCount + 1)..13
}

object ProvinceSchoolCatalog {
    private val configurations = listOf(
        ProvinceSchoolConfiguration("Buenos Aires", 6),
        ProvinceSchoolConfiguration("Catamarca", 6),
        ProvinceSchoolConfiguration("Chubut", 6),
        ProvinceSchoolConfiguration("Córdoba", 6),
        ProvinceSchoolConfiguration("Corrientes", 6),
        ProvinceSchoolConfiguration("Entre Ríos", 6),
        ProvinceSchoolConfiguration("Formosa", 6),
        ProvinceSchoolConfiguration("La Pampa", 6),
        ProvinceSchoolConfiguration("San Juan", 6),
        ProvinceSchoolConfiguration("San Luis", 6),
        ProvinceSchoolConfiguration("Tierra del Fuego", 6),
        ProvinceSchoolConfiguration("Tucumán", 6),
        ProvinceSchoolConfiguration("CABA", 7),
        ProvinceSchoolConfiguration("Chaco", 7),
        ProvinceSchoolConfiguration("Jujuy", 7),
        ProvinceSchoolConfiguration("La Rioja", 7),
        ProvinceSchoolConfiguration("Mendoza", 7),
        ProvinceSchoolConfiguration("Misiones", 7),
        ProvinceSchoolConfiguration("Neuquén", 7),
        ProvinceSchoolConfiguration("Río Negro", 7),
        ProvinceSchoolConfiguration("Salta", 7),
        ProvinceSchoolConfiguration("Santa Cruz", 7),
        ProvinceSchoolConfiguration("Santa Fe", 7),
        ProvinceSchoolConfiguration("Santiago del Estero", 7)
    )

    private val configurationsByProvince = configurations.associateBy(ProvinceSchoolConfiguration::province)

    val provinces: List<String> = configurations.map(ProvinceSchoolConfiguration::province).sorted()

    fun configurationFor(province: String): ProvinceSchoolConfiguration? =
        configurationsByProvince[province]

    fun schoolYearOptionsFor(province: String): List<SchoolYearOption> {
        val configuration = configurationFor(province) ?: return emptyList()

        return buildList {
            configuration.primaryYears.forEach { schoolYear ->
                add(
                    SchoolYearOption(
                        label = "Primary $schoolYear",
                        schoolYear = schoolYear,
                        allowedTracks = setOf(
                            StudentTrack.PRIMARY,
                            StudentTrack.SELF_DIRECTED
                        )
                    )
                )
            }

            configuration.secondaryYears.forEach { schoolYear ->
                add(
                    SchoolYearOption(
                        label = "Secondary $schoolYear",
                        schoolYear = schoolYear,
                        allowedTracks = setOf(
                            StudentTrack.SECONDARY,
                            StudentTrack.TECHNICAL_SECONDARY,
                            StudentTrack.SELF_DIRECTED
                        )
                    )
                )
            }

            add(
                SchoolYearOption(
                    label = "Technical Secondary 13",
                    schoolYear = configuration.technicalSecondaryYears.last,
                    allowedTracks = setOf(StudentTrack.TECHNICAL_SECONDARY)
                )
            )
        }
    }

    fun schoolYearOptionsFor(province: String, track: StudentTrack): List<SchoolYearOption> =
        schoolYearOptionsFor(province)
            .filter { option -> track in option.allowedTracks }
            .mapIndexed { index, option ->
                option.copy(label = ordinalSchoolYearLabel(index + 1))
            }

    fun allowedTracksFor(province: String, schoolYear: Int): Set<StudentTrack> =
        schoolYearOptionsFor(province)
            .firstOrNull { option -> option.schoolYear == schoolYear }
            ?.allowedTracks
            .orEmpty()

    fun isValidSelection(province: String, schoolYear: Int, studentTrack: StudentTrack): Boolean =
        studentTrack in allowedTracksFor(province, schoolYear)

    private fun ordinalSchoolYearLabel(year: Int): String =
        when (year) {
            1 -> "1er Año"
            2 -> "2do Año"
            3 -> "3er Año"
            4 -> "4to Año"
            5 -> "5to Año"
            6 -> "6to Año"
            else -> "$year° Año"
        }
}
