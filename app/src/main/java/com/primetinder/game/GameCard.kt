package com.primetinder.game

data class GameCard(val value: Int) {
    val isPrime: Boolean = PrimeChecker.isPrime(value)
}
